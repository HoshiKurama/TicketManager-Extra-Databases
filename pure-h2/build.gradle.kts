import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.22"
}

group = "com.github.hoshikurama"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")

}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT")
    implementation("com.github.HoshiKurama.TicketManager_API:Paper:10.0.0-RC24")
    implementation("com.github.HoshiKurama.TicketManager_API:Common:10.0.0-RC24")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.22")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<JavaCompile>() {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}