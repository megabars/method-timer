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

        TimingResultWriter.record(sb.toString(), elapsed);
    }
}
