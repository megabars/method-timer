package com.github.methodtimer.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class TimingAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        if (agentArgs == null || agentArgs.isEmpty()) {
            System.err.println("[MethodTimer] No output file path specified.");
            return;
        }

        TimingResultWriter.init(agentArgs);

        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.DISABLED)
                .with(AgentBuilder.FallbackStrategy.Simple.ENABLED)
                .type(new AgentBuilder.RawMatcher() {
                    @Override
                    public boolean matches(TypeDescription typeDescription,
                                           ClassLoader classLoader,
                                           JavaModule module,
                                           Class<?> classBeingRedefined,
                                           ProtectionDomain protectionDomain) {
                        // Инструментируем только классы загруженные из директорий (код пользователя),
                        // а не из JAR-файлов (библиотеки)
                        if (protectionDomain == null) return false;
                        CodeSource codeSource = protectionDomain.getCodeSource();
                        if (codeSource == null) return false;
                        URL location = codeSource.getLocation();
                        if (location == null) return false;
                        String path = location.getPath();
                        return !path.endsWith(".jar") && !path.endsWith(".jar!/");
                    }
                })
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(
                                Advice.to(TimingAdvice.class)
                                        .on(ElementMatchers.isMethod()
                                                .and(ElementMatchers.not(ElementMatchers.isConstructor()))
                                                .and(ElementMatchers.not(ElementMatchers.isSynthetic()))
                                                .and(ElementMatchers.not(ElementMatchers.isBridge()))
                                                .and(ElementMatchers.not(ElementMatchers.isAbstract()))
                                                .and(ElementMatchers.not(ElementMatchers.isNative()))
                                        )
                        )
                )
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
                .installOn(inst);
    }
}
