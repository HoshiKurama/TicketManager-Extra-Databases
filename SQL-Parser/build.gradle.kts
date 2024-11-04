plugins {
    kotlin("jvm")
}

group = "com.github.hoshikurama"
version = "11.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.HoshiKurama.TicketManager_API:Common:11.1.1")
    compileOnly(project(":Common"))
}

kotlin {
    jvmToolchain(21)
}