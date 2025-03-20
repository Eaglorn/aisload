plugins {
    id("java")
    id("application")
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    id("org.springframework.boot") version "2.7.18"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.fku.aisload"
version = "0.0.1"

repositories {
    mavenCentral()
}

application {
    mainClass = "ru.fku.aisload.AisLoadKt"
    applicationName = "AisLoad"
}

kotlin {
    jvmToolchain(8)
}

val kotlinVersion = "2.1.10"
val commonsIoVersion = "2.18.0"
val springVersion = "2.7.18"
val commonsNetVersion = "3.11.1"
val gsonVersion = "2.12.1"
val log4jVersion = "2.24.3"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.springframework.boot:spring-boot-starter:$springVersion") {
        exclude("org.springframework.boot", "spring-boot-starter-logging")
    }
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")
    implementation("commons-net:commons-net:$commonsNetVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
}
