import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.ben-manes.versions") version "0.50.0"
    application
    java
}

application {
    mainClass.set("com.github.hoshikurama.extradatabase.mysql.BukkitPlugin")
}

group = "com.github.hoshikurama"
version = "11.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT")

    compileOnly("com.github.HoshiKurama.TicketManager_API:TMCoroutine:11.0.1")
    compileOnly("com.github.HoshiKurama.TicketManager_API:Common:11.0.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.7.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("com.github.jasync-sql:jasync-mysql:2.2.4")
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
        archiveBaseName.set("TMExtraDB-PureMySQL")

        dependencies {
            exclude { it.moduleGroup == "org.jetbrains.kotlin" }
            exclude { it.moduleGroup == "org.jetbrains.kotlinx" }
        }

        relocate("kotlin", "com.github.hoshikurama.ticketmanager.shaded.kotlin")
        relocate("kotlinx", "com.github.hoshikurama.ticketmanager.shaded.kotlinx")

        relocate("io.netty", "com.github.hoshikurama.extradatabases.shaded.io.netty")
        relocate("com.mysql", "com.github.hoshikurama.extradatabases.shaded.mysql")
        relocate("com.github.jasync", "com.github.hoshikurama.extradatabases.shaded.jasync")
        relocate("io.github.oshai","com.github.hoshikurama.extradatabases.shaded.oshai")
    }
}