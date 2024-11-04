plugins {
    kotlin("jvm")
    id("com.github.ben-manes.versions") version "0.51.0"
}

group = "com.github.hoshikurama"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.HoshiKurama.TicketManager_API:Common:11.1.1")
    compileOnly("org.yaml:snakeyaml:2.3")
}

kotlin {
    jvmToolchain(21)
}