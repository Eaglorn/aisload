plugins {
    id("java")
    id("application")
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    id("org.springframework.boot") version "3.4.3"
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
    jvmToolchain(23)
}

val kotlinVersion = "2.1.10"
val commonsIoVersion = "2.18.0"
val springVersion = "3.4.3"
val commonsNetVersion = "3.11.1"
val gsonVersion = "2.12.1"
val dataFormatYamlVersion = "2.18.3"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.springframework.boot:spring-boot-starter:$springVersion")
    implementation("org.springframework.boot:spring-boot-starter-log4j2:$springVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$dataFormatYamlVersion")
    implementation("commons-net:commons-net:$commonsNetVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
}
