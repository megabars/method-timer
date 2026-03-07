val byteBuddyVersion: String by project

plugins {
    id("java-library")
    id("com.gradleup.shadow") version "8.3.5"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("net.bytebuddy:byte-buddy:$byteBuddyVersion")
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.shadowJar {
    archiveClassifier.set("all")
    relocate("net.bytebuddy", "com.github.methodtimer.agent.shaded.bytebuddy")
    relocate("com.google.gson", "com.github.methodtimer.agent.shaded.gson")

    manifest {
        attributes(
            "Premain-Class" to "com.github.methodtimer.agent.TimingAgent",
            "Can-Retransform-Classes" to "true"
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
