import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
    id("pl.allegro.tech.build.axion-release") version "1.18.18"
    id("com.google.cloud.tools.jib") version "3.4.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
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
}

// Define versions for dependencies to ensure compatibility
val kotestVersion = "5.8.0"
val ktorVersion = "2.3.7"
val arrowVersion = "1.2.1"
val exposedVersion = "0.61.0"

dependencies {
    // Arrow for functional programming
    implementation("io.arrow-kt:arrow-core:$arrowVersion")

    // Ktor server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.mongodb:mongo-java-driver:3.12.14")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.typesafe:config:1.4.3")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Exposed ORM
    //TODO: remove any unneeded dependencies, do we need jdbc and dao?
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

    // Testing
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.arrow-kt:arrow-core:$arrowVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    testImplementation("org.testcontainers:mongodb:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:2.0.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.flywaydb:flyway-core:11.10.2")
    testImplementation("org.flywaydb:flyway-database-postgresql:11.10.2")
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

// Configure ktlint
ktlint {
    version.set("1.0.1")
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

// Configure Detekt
detekt {
    config.setFrom(files("$projectDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = true
    baseline = file("$projectDir/config/detekt/baseline.xml")
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
    baseline.set(file("$projectDir/config/detekt/baseline.xml"))
}
