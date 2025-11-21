plugins {
    kotlin("jvm") version "2.2.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.wire:wire-apps-jvm-sdk:0.0.18")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}