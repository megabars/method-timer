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
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveClassifier.set("all")
    relocate("net.bytebuddy", "com.github.methodtimer.agent.shaded.bytebuddy")

    manifest {
        attributes(
            "Premain-Class" to "com.github.methodtimer.agent.TimingAgent"
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
