package io.sdkman.broker.domain.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class ArchiveTypeSpec :
    ShouldSpec({

        context("ArchiveType.fromUrl") {

            should("resolve .zip URLs to Zip") {
                ArchiveType.fromUrl("https://example.com/foo-1.0.0.zip") shouldBe ArchiveType.Zip
            }

            should("resolve .tar.gz URLs to TarGz") {
                ArchiveType.fromUrl("https://example.com/foo-1.0.0.tar.gz") shouldBe ArchiveType.TarGz
            }

            should("resolve .tgz URLs to TarGz") {
                ArchiveType.fromUrl("https://example.com/foo-1.0.0.tgz") shouldBe ArchiveType.TarGz
            }

            should("resolve .tar.bz2 URLs to TarBz2") {
                ArchiveType.fromUrl("https://example.com/foo-1.0.0.tar.bz2") shouldBe ArchiveType.TarBz2
            }

            should("resolve .xz URLs to Xz") {
                ArchiveType.fromUrl("https://example.com/foo-1.0.0.xz") shouldBe ArchiveType.Xz
            }

            should("default to Zip for unrecognised extensions") {
                ArchiveType.fromUrl("https://example.com/foo-1.0.0.bin") shouldBe ArchiveType.Zip
            }

            should("expose the spec-defined string value for each archive type") {
                ArchiveType.Zip.value shouldBe "zip"
                ArchiveType.TarGz.value shouldBe "tar.gz"
                ArchiveType.TarBz2.value shouldBe "tar.bz2"
                ArchiveType.Xz.value shouldBe "xz"
            }
        }
    })
