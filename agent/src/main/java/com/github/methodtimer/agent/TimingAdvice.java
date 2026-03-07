package com.github.methodtimer.agent;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class TimingAdvice {

    private static final ConcurrentHashMap<Method, String> fqnCache = new ConcurrentHashMap<>();

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
        String fqn = fqnCache.get(method);
        if (fqn == null) {
            fqn = buildFqn(method);
            fqnCache.put(method, fqn);
        }
        TimingResultWriter.record(fqn, elapsed);
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
}
