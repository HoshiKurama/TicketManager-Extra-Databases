plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "8.3.5"
    id("com.github.ben-manes.versions") version "0.51.0"
    application
}

application {
    mainClass.set("com.github.hoshikurama.extradatabase.mysql.BukkitPlugin")
}

group = "com.github.hoshikurama"
version = "11.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")

    compileOnly("com.github.HoshiKurama.TicketManager_API:TMCoroutine:11.1.1")
    compileOnly("com.github.HoshiKurama.TicketManager_API:Common:11.1.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    implementation("com.mysql:mysql-connector-j:9.1.0")
    implementation("com.github.jasync-sql:jasync-mysql:2.2.4")
    implementation(project(":Common"))
    implementation(project(":SQL-Parser"))
}

kotlin {
    jvmToolchain(21)
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