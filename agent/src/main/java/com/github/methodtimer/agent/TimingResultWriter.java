package com.github.methodtimer.agent;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimingResultWriter {

    private static final ConcurrentHashMap<String, Long> timings = new ConcurrentHashMap<>();
    // Кеш перенесён сюда из TimingAdvice: поля Advice недоступны из инлайненного байткода
    private static final ConcurrentHashMap<Method, String> fqnCache = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
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
                    flush();
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
            fqnCache.put(method, fqn);
        }
        timings.put(fqn, nanos);
    }

    private static String buildFqn(Method method) {
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

        Path path = Paths.get(outputFilePath);
        Path tmpPath = Paths.get(outputFilePath + ".tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(tmpPath, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, Long> entry : timings.entrySet()) {
                TimingEntry te = new TimingEntry(entry.getKey(), entry.getValue());
                writer.write(gson.toJson(te));
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

    private static class TimingEntry {
        final String fqn;
        final long timeNs;

        TimingEntry(String fqn, long timeNs) {
            this.fqn = fqn;
            this.timeNs = timeNs;
        }
    }
}
