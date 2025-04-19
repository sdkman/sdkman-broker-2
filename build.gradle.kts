import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
}

group = "io.sdkman"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// Define versions for dependencies to ensure compatibility
val kotestVersion = "5.8.0"
val ktorVersion = "2.3.7"
val arrowVersion = "1.2.1"

dependencies {
    // Arrow for functional programming
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    
    // Ktor server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    
    // MongoDB
    implementation("org.litote.kmongo:kmongo:4.11.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.arrow-kt:arrow-core:$arrowVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.testcontainers:mongodb:1.19.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xskip-metadata-version-check")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxHeapSize = "512m"
}

application {
    mainClass.set("io.sdkman.broker.ApplicationKt")
} 