import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
    java
}

application {
    mainClass.set("com.github.hoshikurama.extradatabase.h2.PaperPlugin")
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

    compileOnly("com.github.HoshiKurama.TicketManager_API:Paper:10.0.0-RC29")
    compileOnly("com.github.HoshiKurama.TicketManager_API:Common:10.0.0-RC29")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22")
    compileOnly("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")

    implementation("com.github.seratch:kotliquery:1.9.0")
    implementation("com.h2database:h2:2.1.214")
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
        dependencies {
            // Provided by Paper
            exclude { it.moduleGroup.startsWith("com.google") }
            exclude { it.moduleGroup.startsWith("org.jetbrains") }
            exclude(dependency("org.slf4j:.*:.*"))
            exclude(dependency("org.jetbrains:annotations:.*"))
            exclude(dependency("org.jetbrains.kotlin:.*:.*"))
            exclude(dependency("joda-time:joda-time:.*"))
        }

        // Provided by TicketManager
        relocate("kotlin", "com.github.hoshikurama.ticketmanager.shaded.kotlin")
        relocate("kotlinx", "com.github.hoshikurama.ticketmanager.shaded.kotlinx")
        relocate("org.joda.time", "com.github.hoshikurama.ticketmanager.shaded.jodatime")

        relocate("kotliquery", "com.github.hoshikurama.extradatabases.shaded.kotliquery")
    }
}