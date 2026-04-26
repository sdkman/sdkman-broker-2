package io.sdkman.broker.domain.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class ArchiveTypeSpec :
    ShouldSpec({
        context("ArchiveType.fromUrl") {

            should("resolve .zip URLs to Zip") {
                ArchiveType.fromUrl("https://example.com/groovy-4.0.0.zip") shouldBe ArchiveType.Zip
            }

            should("resolve .tar.gz URLs to TarGz") {
                ArchiveType.fromUrl("https://example.com/temurin-17.0.2.tar.gz") shouldBe ArchiveType.TarGz
            }

            should("resolve .tgz URLs to TarGz") {
                ArchiveType.fromUrl("https://example.com/spark-2.3.1.tgz") shouldBe ArchiveType.TarGz
            }

            should("resolve .tar.bz2 URLs to TarBz2") {
                ArchiveType.fromUrl("https://example.com/scala-2.13.10.tar.bz2") shouldBe ArchiveType.TarBz2
            }

            should("resolve .xz URLs to Xz") {
                ArchiveType.fromUrl("https://example.com/jdk-21.0.1.xz") shouldBe ArchiveType.Xz
            }

            should("default to Zip for unknown extensions") {
                ArchiveType.fromUrl("https://example.com/mystery-1.0.0.bin") shouldBe ArchiveType.Zip
            }

            should("expose tar.gz as the header value") {
                ArchiveType.TarGz.value shouldBe "tar.gz"
            }

            should("expose tar.bz2 as the header value") {
                ArchiveType.TarBz2.value shouldBe "tar.bz2"
            }

            should("expose xz as the header value") {
                ArchiveType.Xz.value shouldBe "xz"
            }

            should("expose zip as the header value") {
                ArchiveType.Zip.value shouldBe "zip"
            }
        }
    })
