import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
    java
}

application {
    mainClass.set("com.github.hoshikurama.extradatabase.mysql.BukkitPlugin")
}

group = "com.github.hoshikurama"
version = "10.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT")

    compileOnly("com.github.HoshiKurama.TicketManager_API:Paper:10.0.0-RC30")
    compileOnly("com.github.HoshiKurama.TicketManager_API:Common:10.0.0-RC30")
    compileOnly("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.2")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")

    implementation("com.mysql:mysql-connector-j:8.0.32")
    implementation("com.github.jasync-sql:jasync-mysql:2.1.23")
    implementation(project(":Common"))
    implementation(project(":SQL-Parser"))
}

kotlin {

}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

tasks {
    shadowJar {
        dependencies {
            // Provided by Paper
            exclude { it.moduleGroup.startsWith("com.google") }
            exclude { it.moduleGroup.startsWith("org.jetbrains") }
            exclude(dependency("org.slf4j:.*:.*"))
            exclude(dependency("org.jetbrains:annotations:.*"))
            exclude(dependency("org.jetbrains.kotlin:.*:.*"))
            exclude(dependency("org.jetbrains.kotlinx:.*:.*"))
        }

        // Provided by TicketManager
        relocate("kotlin", "com.github.hoshikurama.ticketmanager.shaded.kotlin")
        relocate("kotlinx", "com.github.hoshikurama.ticketmanager.shaded.kotlinx")
        relocate("io.netty", "com.github.hoshikurama.extradatabases.shaded.io.netty")
        relocate("com.mysql", "com.github.hoshikurama.extradatabases.shaded.mysql")
        relocate("com.github.jasync", "com.github.hoshikurama.extradatabases.shaded.jasync")
        relocate("mu", "com.github.hoshikurama.extradatabases.shaded.mu")
    }
}