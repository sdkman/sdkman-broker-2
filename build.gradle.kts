plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
    id("pl.allegro.tech.build.axion-release") version "1.18.18"
    id("com.google.cloud.tools.jib") version "3.4.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "io.sdkman"
// Use Axion Release Plugin to manage version
version = scmVersion.version

// Configure the plugin
scmVersion {
    tag {
        prefix = "v"
        versionSeparator = ""
    }
    checks {
        uncommittedChanges = true
        aheadOfRemote = true
    }
}

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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
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

// Configure Jib for Docker image building
jib {
    from {
        image = "eclipse-temurin:21-jre-alpine"
    }
    to {
        image = "registry.digitalocean.com/sdkman/sdkman-broker"
        tags = setOf(version.toString(), "latest")
    }
    container {
        ports = listOf("8080")
        mainClass = application.mainClass.get()
        jvmFlags = listOf("-Xms256m", "-Xmx512m")
        // Add these environment variables as defaults, but they can be overridden at runtime
        environment =
            mapOf(
                "MONGODB_URI" to "mongodb://localhost:27017",
                "MONGODB_DATABASE" to "sdkman",
            )
        // A sensible default for production containers
        user = "1000"
    }
}

// Configure ktlint
ktlint {
    version.set("1.0.1")
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

// Configure Detekt
detekt {
    toolVersion = "1.23.8"
    config = files("config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = true
    baseline = file("config/detekt/baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "21"
    baseline.set(file("config/detekt/baseline.xml"))
}
