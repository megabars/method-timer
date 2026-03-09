package com.github.methodtimer.agent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimingResultWriter {

    private static final ConcurrentHashMap<String, Long> timings = new ConcurrentHashMap<>();
    // Кеш перенесён сюда из TimingAdvice: поля Advice недоступны из инлайненного байткода
    private static final ConcurrentHashMap<Method, String> fqnCache = new ConcurrentHashMap<>();
    // Ограничение кеша — защита от неограниченного роста в долгоживущих процессах
    private static final int FQN_CACHE_MAX_SIZE = 50_000;
    private static volatile String outputFilePath;

    public static void init(String filePath) {
        outputFilePath = filePath;

        // Сбрасываем в файл каждые 2 секунды — для долгоживущих процессов (Spring Boot и т.д.)
        Thread flushThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    flush();
                } catch (InterruptedException e) {
                    // Не вызываем flush() — shutdown hook сделает это
                    return;
                }
            }
        }, "method-timer-flush");
        flushThread.setDaemon(true);
        flushThread.start();

        // Также сбрасываем при завершении JVM
        Runtime.getRuntime().addShutdownHook(new Thread(TimingResultWriter::flush, "method-timer-shutdown"));
    }

    public static void record(Method method, long nanos) {
        String fqn = fqnCache.get(method);
        if (fqn == null) {
            fqn = buildFqn(method);
            // Не кешируем при превышении лимита — защита от OOM в приложениях с динамической генерацией классов
            if (fqnCache.size() < FQN_CACHE_MAX_SIZE) {
                fqnCache.put(method, fqn);
            }
        }
        timings.put(fqn, nanos);
    }

    static String buildFqn(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        String className = declaringClass.getCanonicalName();
        if (className == null) {
            className = declaringClass.getName().replace('$', '.');
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        StringBuilder sb = new StringBuilder();
        sb.append(className).append('.').append(method.getName()).append('(');
        for (int i = 0; i < paramTypes.length; i++) {
            if (i > 0) sb.append(", ");
            String paramName = paramTypes[i].getCanonicalName();
            if (paramName == null) {
                paramName = paramTypes[i].getName().replace('$', '.');
            }
            sb.append(paramName);
        }
        sb.append(')');
        return sb.toString();
    }

    private static synchronized void flush() {
        if (outputFilePath == null || timings.isEmpty()) {
            return;
        }

        // Снимок для консистентной записи — исключает race condition с record()
        Map<String, Long> snapshot = new HashMap<>(timings);

        Path path = Paths.get(outputFilePath);
        Path tmpPath = Paths.get(outputFilePath + ".tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(tmpPath, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, Long> entry : snapshot.entrySet()) {
                writer.write(toJson(entry.getKey(), entry.getValue()));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("[MethodTimer] Failed to write timing results: " + e.getMessage());
            return;
        }

        try {
            Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // Fallback: не все FS поддерживают ATOMIC_MOVE
            try {
                Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                System.err.println("[MethodTimer] Failed to move timing results: " + ex.getMessage());
            }
        }
    }

    // КОНТРАКТ: порядок полей ("fqn" перед "timeNs") фиксирован — TimingResultsReader (плагин)
    // парсит строку через regex и не зависит от порядка полей, но изменение формата
    // должно синхронно обновляться в FQN_REGEX / TIME_NS_REGEX на стороне плагина.
    static String toJson(String fqn, long timeNs) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"fqn\":\"");
        // Экранируем все спецсимволы JSON согласно RFC 8259
        for (int i = 0; i < fqn.length(); i++) {
            char c = fqn.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                default:
                    if (c < 0x20) {
                        // Прочие управляющие символы (U+0000..U+001F) — unicode escape
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\",\"timeNs\":").append(timeNs).append('}');
        return sb.toString();
    }
}
