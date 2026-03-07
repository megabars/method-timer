plugins {
    id("java")
    kotlin("jvm") version "1.9.25" apply false
}

allprojects {
    group = "com.github.methodtimer"
    version = "1.1.0"

    repositories {
        mavenCentral()
    }
}
