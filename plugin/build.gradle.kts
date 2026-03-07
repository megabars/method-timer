val platformVersion: String by project
val pluginVersion: String by project

plugins {
    id("org.jetbrains.intellij.platform") version "2.2.1"
    kotlin("jvm") version "1.9.25"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(platformVersion)
        bundledPlugin("com.intellij.java")
        instrumentationTools()
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.methodtimer"
        name = "Method Timer"
        version = pluginVersion
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    publishing {
        token = providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN")
    }
}

val agentJar = project(":agent").tasks.named("shadowJar")

tasks.prepareSandbox {
    dependsOn(agentJar)
    from(agentJar) {
        into("${intellijPlatform.projectName.get()}/agent")
    }
}
