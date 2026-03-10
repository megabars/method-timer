plugins {
    id("java")
    kotlin("jvm") version "1.9.25" apply false
}

val pluginVersion: String by project

allprojects {
    group = "com.github.methodtimer"
    version = pluginVersion

    repositories {
        mavenCentral()
    }
}
