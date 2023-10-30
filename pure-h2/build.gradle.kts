import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.ben-manes.versions") version "0.47.0"
    application
    java
}

application {
    mainClass.set("com.github.hoshikurama.extradatabase.h2.BukkitPlugin")
}

group = "com.github.hoshikurama"
version = "11.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")

}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT")

    compileOnly("com.github.HoshiKurama.TicketManager_API:Common:11.0.0-RC3")
    compileOnly("com.github.HoshiKurama.TicketManager_API:TMCoroutine:11.0.0-RC3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.10")

    implementation("com.github.seratch:kotliquery:1.9.0")
    implementation("com.h2database:h2:2.2.220")
    implementation(project(":Common"))
    implementation(project(":SQL-Parser"))
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
        configurations = listOf(project.configurations.runtimeClasspath.get())
        archiveBaseName.set("TMExtraDB-PureH2")

        dependencies {
            exclude { it.moduleGroup == "org.jetbrains.kotlin" }
        }

        relocate("kotlin", "com.github.hoshikurama.ticketmanager.shaded.kotlin")
        relocate("kotlinx", "com.github.hoshikurama.ticketmanager.shaded.kotlinx")

        relocate("kotliquery", "com.github.hoshikurama.extradatabases.shaded.kotliquery")
        relocate("com.zaxxer.hikari", "com.github.hoshikurama.extradatabases.shaded.hikari")
        relocate("org.h2", "com.github.hoshikurama.extradatabases.shaded.h2")
    }
}