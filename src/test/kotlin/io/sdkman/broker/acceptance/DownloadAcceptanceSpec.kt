package io.sdkman.broker.acceptance

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.*
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.server.testing.testApplication
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.TestDependencyInjection
import org.bson.Document
import org.bson.types.ObjectId

class DownloadAcceptanceSpec : ShouldSpec({
    listener(MongoTestListener)

    context("GET /download/{candidate}/{version}/{platform}") {
        
        should("return 302 redirect with checksum headers for exact platform match") {
            // Given
            val testData = Document().apply {
                put("_id", ObjectId())
                put("candidate", "java")
                put("version", "17.0.2-tem")
                put("platform", "LinuxX64")
                put("url", "https://example.com/java-17.0.2-linux-x64.tar.gz")
                put("vendor", "tem")
                put("visible", true)
                put("checksums", Document().apply {
                    put("sha256", "abc123def456")
                    put("sha1", "def456ghi789")
                })
            }
            MongoTestListener.database.getCollection("versions").insertOne(testData)

            testApplication {
                application {
                    TestDependencyInjection.run { configureApplication() }
                }
                
                val client = createClient {
                    followRedirects = false // Don't follow redirects to external URLs
                }
                
                // When
                val response = client.get("/download/java/17.0.2-tem/linuxx64")
                
                // Then
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://example.com/java-17.0.2-linux-x64.tar.gz"
                response.headers["X-Sdkman-Checksum-SHA256"] shouldBe "abc123def456"
                response.headers["X-Sdkman-Checksum-SHA1"] shouldBe "def456ghi789"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "tar.gz"
            }
        }

        should("return 302 redirect with UNIVERSAL fallback when platform-specific version not found") {
            // Given
            val testData = Document().apply {
                put("_id", ObjectId())
                put("candidate", "groovy")
                put("version", "4.0.0")
                put("platform", "UNIVERSAL")
                put("url", "https://example.com/groovy-4.0.0.zip")
                put("checksums", Document().apply {
                    put("sha256", "universal123")
                })
            }
            MongoTestListener.database.getCollection("versions").insertOne(testData)

            testApplication {
                application {
                    TestDependencyInjection.run { configureApplication() }
                }
                
                val client = createClient {
                    followRedirects = false
                }
                
                // When
                val response = client.get("/download/groovy/4.0.0/linuxx64")
                
                // Then
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://example.com/groovy-4.0.0.zip"
                response.headers["X-Sdkman-Checksum-SHA256"] shouldBe "universal123"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
            }
        }

        should("prefer exact platform match over UNIVERSAL") {
            // Given - both platform-specific and universal versions exist
            val platformSpecific = Document().apply {
                put("_id", ObjectId())
                put("candidate", "kotlin")
                put("version", "1.8.0")
                put("platform", "LinuxX64")
                put("url", "https://example.com/kotlin-linux-x64.zip")
                put("checksums", Document().apply {
                    put("sha256", "platform123")
                })
            }
            val universal = Document().apply {
                put("_id", ObjectId())
                put("candidate", "kotlin")
                put("version", "1.8.0")
                put("platform", "UNIVERSAL")
                put("url", "https://example.com/kotlin-universal.zip")
                put("checksums", Document().apply {
                    put("sha256", "universal123")
                })
            }
            val collection = MongoTestListener.database.getCollection("versions")
            collection.insertOne(platformSpecific)
            collection.insertOne(universal)

            testApplication {
                application {
                    TestDependencyInjection.run { configureApplication() }
                }
                
                val client = createClient {
                    followRedirects = false
                }
                
                // When
                val response = client.get("/download/kotlin/1.8.0/linuxx64")
                
                // Then - should get platform-specific version
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://example.com/kotlin-linux-x64.zip"
                response.headers["X-Sdkman-Checksum-SHA256"] shouldBe "platform123"
            }
        }

        should("return 400 Bad Request for invalid platform") {
            testApplication {
                application {
                    TestDependencyInjection.run { configureApplication() }
                }
                
                val client = createClient {
                    followRedirects = false
                }
                
                // When
                val response = client.get("/download/java/17.0.2/invalidplatform")
                
                // Then
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        should("return 404 Not Found when candidate/version does not exist") {
            testApplication {
                application {
                    TestDependencyInjection.run { configureApplication() }
                }
                
                val client = createClient {
                    followRedirects = false
                }
                
                // When
                val response = client.get("/download/nonexistent/1.0.0/linuxx64")
                
                // Then
                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        should("return 404 Not Found when no platform-specific or UNIVERSAL version exists") {
            // Given - only a different platform version exists
            val testData = Document().apply {
                put("_id", ObjectId())
                put("candidate", "java")
                put("version", "17.0.2-tem")
                put("platform", "WindowsX64")
                put("url", "https://example.com/java-windows.zip")
            }
            MongoTestListener.database.getCollection("versions").insertOne(testData)

            testApplication {
                application {
                    TestDependencyInjection.run { configureApplication() }
                }
                
                val client = createClient {
                    followRedirects = false
                }
                
                // When
                val response = client.get("/download/java/17.0.2-tem/linuxx64")
                
                // Then
                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        should("handle multiple checksum algorithms with correct priority ordering") {
            // Given
            val testData = Document().apply {
                put("_id", ObjectId())
                put("candidate", "gradle")
                put("version", "7.6")
                put("platform", "UNIVERSAL")
                put("url", "https://example.com/gradle-7.6.zip")
                put("checksums", Document().apply {
                    put("md5", "md5hash")
                    put("sha1", "sha1hash")
                    put("sha256", "sha256hash")
                    put("sha512", "sha512hash")
                })
            }
            MongoTestListener.database.getCollection("versions").insertOne(testData)

            testApplication {
                application {
                    TestDependencyInjection.run { configureApplication() }
                }
                
                val client = createClient {
                    followRedirects = false
                }
                
                // When
                val response = client.get("/download/gradle/7.6/linuxx64")
                
                // Then
                response.status shouldBe HttpStatusCode.Found
                response.headers["X-Sdkman-Checksum-SHA256"] shouldBe "sha256hash"
                response.headers["X-Sdkman-Checksum-SHA512"] shouldBe "sha512hash"
                response.headers["X-Sdkman-Checksum-SHA1"] shouldBe "sha1hash"
                response.headers["X-Sdkman-Checksum-MD5"] shouldBe "md5hash"
            }
        }

        should("detect archive type correctly for different file extensions") {
            // Given
            val testCases = listOf(
                "https://example.com/file.zip" to "zip",
                "https://example.com/file.tar.gz" to "tar.gz",
                "https://example.com/file.tgz" to "tar.gz",
                "https://example.com/file.tar.bz2" to "tar.bz2",
                "https://example.com/file.tar.xz" to "tar.xz",
                "https://example.com/file.unknown" to "zip" // default fallback
            )

            testCases.forEachIndexed { index, (url, expectedType) ->
                val testData = Document().apply {
                    put("_id", ObjectId())
                    put("candidate", "test$index")
                    put("version", "1.0.0")
                    put("platform", "UNIVERSAL")
                    put("url", url)
                }
                MongoTestListener.database.getCollection("versions").insertOne(testData)

                testApplication {
                    application {
                        TestDependencyInjection.run { configureApplication() }
                    }
                    
                    val client = createClient {
                        followRedirects = false
                    }
                    
                    // When
                    val response = client.get("/download/test$index/1.0.0/linuxx64")
                    
                    // Then
                    response.status shouldBe HttpStatusCode.Found
                    response.headers["X-Sdkman-ArchiveType"] shouldBe expectedType
                }
            }
        }

        should("handle all valid platform identifiers") {
            val platforms = listOf(
                "linuxx64" to "LinuxX64",
                "linuxarm64" to "LinuxARM64", 
                "linuxx32" to "LinuxX32",
                "darwinx64" to "DarwinX64",
                "darwinarm64" to "DarwinARM64",
                "windowsx64" to "WindowsX64",
                "exotic" to "Exotic"
            )

            platforms.forEachIndexed { index, (urlParam, normalizedId) ->
                val testData = Document().apply {
                    put("_id", ObjectId())
                    put("candidate", "platform-test$index")
                    put("version", "1.0.0")
                    put("platform", normalizedId)
                    put("url", "https://example.com/platform-$index.zip")
                }
                MongoTestListener.database.getCollection("versions").insertOne(testData)

                testApplication {
                    application {
                        TestDependencyInjection.run { configureApplication() }
                    }
                    
                    val client = createClient {
                        followRedirects = false
                    }
                    
                    // When
                    val response = client.get("/download/platform-test$index/1.0.0/$urlParam")
                    
                    // Then
                    response.status shouldBe HttpStatusCode.Found
                    response.headers["Location"] shouldBe "https://example.com/platform-$index.zip"
                }
            }
        }
    }

    beforeEach {
        MongoTestListener.database.getCollection("versions").drop()
    }
})