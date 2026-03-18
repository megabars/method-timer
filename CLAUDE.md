# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Язык общения: русский. Все ответы, комментарии и объяснения — только на русском языке.**

## Обзор проекта

Плагин "Method Timer" для IntelliJ IDEA — замеряет время выполнения Java-методов через Java Agent и отображает результаты как Code Vision линзы над объявлениями методов. Работает как с короткоживущими консольными приложениями, так и с долгоживущими (Spring Boot и т.д.).

## Команды сборки и тестирования

```bash
./gradlew :agent:shadowJar        # Собрать fat JAR агента (agent/build/libs/agent-*-all.jar)
./gradlew :plugin:compileKotlin   # Скомпилировать только плагин
./gradlew :plugin:buildPlugin     # Полная сборка плагина с ZIP-дистрибутивом
./gradlew :plugin:runIde          # Запустить sandbox IntelliJ с установленным плагином

./gradlew :agent:test             # Тесты агента (JUnit 5)
./gradlew :plugin:test            # Тесты плагина (IntelliJ test framework + JUnit 5)
./gradlew :plugin:verifyPlugin    # Проверить совместимость с 2024.3, 2025.1, 2025.2

# Публикация на JetBrains Marketplace (требует JETBRAINS_MARKETPLACE_TOKEN)
./gradlew :plugin:publishPlugin
```

Сборка плагина автоматически запускает shadowJar агента через зависимость задачи `prepareSandbox`. JAR агента размещается в `<plugin-name>/agent/` внутри sandbox.

ZIP-дистрибутив: `plugin/build/distributions/plugin-<version>.zip`

## Архитектура

Два Gradle-модуля, связанных конвейером данных:

**agent/** (Java 8, ByteBuddy) — Автономный JAR, подключаемый к JVM пользователя через `-javaagent`. Инструментирует **только код пользователя** (классы из директорий, не из JAR-файлов) хуками `System.nanoTime()` на входе/выходе. Daemon-поток сбрасывает результаты в JSONL каждые 2 секунды + shutdown hook при завершении.

**plugin/** (Kotlin, IntelliJ Platform SDK) — Внедряет агент в конфигурации запуска, периодически читает результаты (polling каждые 3 секунды), отображает через Code Vision.

### Поток данных

```
JavaProgramPatcher (внедряет -javaagent:agent.jar=output.jsonl)
  → JVM пользователя работает с инструментацией агента
  → Daemon-поток агента пишет {fqn, timeNs} JSONL каждые 2 сек
  → TimingProcessListener polling каждые 3 сек + финальное чтение при завершении
  → MethodTimingStorage (персистится в methodTimingData.xml)
  → CodeVisionHost.invalidateProvider() обновляет линзы
```

### Версии и совместимость

- Плагин: Kotlin JVM 21, `sinceBuild = "243"` (IntelliJ 2024.3+)
- Агент: Java 8 source/target — запускается на JVM 8+
- Тестируемые платформы: Community 2024.1, 2024.3, 2025.1, 2025.2

### Формат JSONL-файла

Агент пишет одну запись на строку: `{"fqn":"<escaped>","timeNs":<digits>}`

Порядок полей **фиксирован** — плагин парсит regex-ом, а не JSON-парсером. Запись атомарна: агент пишет в `.tmp`, потом `Files.move(REPLACE_EXISTING, ATOMIC_MOVE)`.

`MethodTimingStorage` хранит только **последнее** измерение на метод (Map<FQN, timeNs>). История не накапливается.

### Критический контракт сопоставления сигнатур

Агент и плагин должны генерировать идентичные FQN-строки методов. Формат: `com.example.ClassName.methodName(java.lang.String, int)`

- **Сторона агента** (`TimingAdvice.java`): `Class.getCanonicalName() + "." + method.getName() + "(" + paramTypes + ")"`
- **Сторона плагина** (`MethodSignatureResolver.kt`): `PsiMethod.containingClass.qualifiedName + "." + name + "(" + params.type.canonicalText + ")"`

Вложенные классы: агент использует `getCanonicalName()` (точки, а не `$`).

### Фильтрация классов агентом

Агент определяет пользовательский код через `ProtectionDomain.getCodeSource().getLocation()`:
- Путь **не заканчивается** на `.jar` → код пользователя → инструментируем
- Путь **заканчивается** на `.jar` → библиотека → пропускаем

Это надёжнее чёрного списка пакетов — автоматически исключает все библиотеки.

### Обновление Code Vision

- `DaemonCodeAnalyzer.restart()` **не работает** для обновления Code Vision
- Нужно использовать `CodeVisionHost.invalidateProvider(LensInvalidateSignal)` — принудительная инвалидация конкретного провайдера

### Extension Points (plugin.xml)

- `java.programPatcher` — внедряет агент в JVM-аргументы для запусков `ModuleBasedConfiguration`
- `codeInsight.codeVisionProvider` + `codeInsight.codeVisionProviderFactory` — Code Vision линзы
- `applicationListeners` — `ExecutionListener` для `processStarted` (polling + ProcessAdapter на завершение)

### Ключевые ограничения

- `JavaProgramPatcher` работает только для прямого запуска Java/JUnit, не для Gradle-делегированного выполнения
- JAR агента использует Shadow plugin с перемещением ByteBuddy и Gson для избежания конфликтов classpath
- `MethodTimingStorage` — light service (`@Service`) — НЕ регистрировать дополнительно в plugin.xml
- `TimingRunTracker` связывает имена конфигураций запуска с путями к temp-файлам между patcher и listener
- `AgentBuilder.RedefinitionStrategy.DISABLED` + `FallbackStrategy.Simple.ENABLED` — безопасный режим для совместимости с Spring Boot и другими фреймворками
