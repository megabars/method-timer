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
            // @Advice.Origin Method инжектирует объект Method через рефлексию при первом вызове.
            // Альтернатива @Advice.Origin("#t.#m(#s)") даёт строку без рефлексии, но ByteBuddy
            // разделяет параметры через "," без пробела, тогда как контракт FQN требует ", " —
            // смена потребует обновления MethodSignatureResolver на стороне плагина.
            // FQN-кеш в TimingResultWriter снижает накладные расходы до единоразового lookup.
            @Advice.Origin Method method
    ) {
        long elapsed = System.nanoTime() - startTime;
        // Делегируем всё в TimingResultWriter — вызов public-метода безопасен при инлайнинге
        TimingResultWriter.record(method, elapsed);
    }
}
