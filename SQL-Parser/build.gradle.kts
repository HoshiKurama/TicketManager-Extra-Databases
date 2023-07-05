import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.22"
    java
}

group = "com.github.hoshikurama"
version = "10.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.HoshiKurama.TicketManager_API:Paper:10.0.0-RC29")
    compileOnly("com.github.HoshiKurama.TicketManager_API:Common:10.0.0-RC29")
    compileOnly(project(":Common"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}
