plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    alias(libs.plugins.axion.release)
    alias(libs.plugins.jib)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
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

// Add task to generate release.properties file
tasks.register("generateReleaseProperties") {
    description = "Generates release.properties file with the current project version"
    group = "build"

    inputs.property("version", version)
    val resourcesDir = tasks.processResources.get().destinationDir
    outputs.file(File(resourcesDir, "release.properties"))

    doLast {
        resourcesDir.mkdirs()
        File(resourcesDir, "release.properties").writeText("release=${project.version}")
    }
}

// Make processResources depend on generateReleaseProperties
tasks.processResources {
    dependsOn("generateReleaseProperties")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    // Arrow for functional programming
    implementation(libs.arrow.core)

    // Ktor server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.mongo.java.driver)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.typesafe.config)
    implementation(libs.logback.classic)

    // Exposed ORM
    implementation(libs.exposed.core) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }
    implementation(libs.exposed.jdbc) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }
    implementation(libs.exposed.kotlin.datetime) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }

    // Testing
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.arrow.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.okhttp)
    testImplementation(libs.testcontainers.mongodb)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.kotest.extensions.testcontainers)
    testImplementation(libs.mockk)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.database.postgresql)

    detektPlugins(libs.detekt.rules)
    compileOnly(libs.detekt.rules)
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
    maxParallelForks = 1
}

application {
    mainClass.set("io.sdkman.broker.App")
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
                "MONGODB_DATABASE" to "sdkman"
            )
        // A sensible default for production containers
        user = "1000"
    }
}

ktlint {
    version.set("1.5.0")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$projectDir/detekt.yml"))
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}
