package com.github.methodtimer.agent;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimingResultWriter {

    private static final ConcurrentHashMap<String, Long> timings = new ConcurrentHashMap<>();
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

    public static void record(String fqn, long nanos) {
        timings.put(fqn, nanos);
    }

    private static synchronized void flush() {
        if (outputFilePath == null || timings.isEmpty()) {
            return;
        }

        Path path = Paths.get(outputFilePath);
        Gson gson = new Gson();

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, Long> entry : timings.entrySet()) {
                TimingEntry te = new TimingEntry(entry.getKey(), entry.getValue());
                writer.write(gson.toJson(te));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("[MethodTimer] Failed to write timing results: " + e.getMessage());
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
