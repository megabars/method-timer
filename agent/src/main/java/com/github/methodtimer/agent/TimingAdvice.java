package com.github.methodtimer.agent;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

public class TimingAdvice {

    @Advice.OnMethodEnter
    public static long onEnter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
            @Advice.Enter long startTime,
            @Advice.Origin Method method
    ) {
        long elapsed = System.nanoTime() - startTime;
        // Делегируем всё в TimingResultWriter — вызов public-метода безопасен при инлайнинге
        TimingResultWriter.record(method, elapsed);
    }
}
