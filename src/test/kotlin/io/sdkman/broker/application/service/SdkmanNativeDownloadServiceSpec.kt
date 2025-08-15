package io.sdkman.broker.application.service

import io.kotest.core.spec.style.ShouldSpec
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.support.shouldBeLeft
import io.sdkman.broker.support.shouldBeRightAnd

class SdkmanNativeDownloadServiceSpec : ShouldSpec({
    val underTest = SdkmanNativeDownloadServiceImpl()

    context("downloadNativeCli") {
        context("with valid inputs") {
            should("return download info for linuxx64 platform") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "linuxx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl ==
                        "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                        "v0.7.4/sdkman-cli-native-0.7.4-x86_64-unknown-linux-gnu.zip" &&
                        downloadInfo.archiveType == "zip"
                }
            }

            should("return download info for darwinarm64 platform") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "darwinarm64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl ==
                        "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                        "v0.7.4/sdkman-cli-native-0.7.4-aarch64-apple-darwin.zip" &&
                        downloadInfo.archiveType == "zip"
                }
            }

            should("return download info for windowsx64 platform") {
                val result = underTest.downloadNativeCli("install", "0.8.0", "windowsx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl ==
                        "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                        "v0.8.0/sdkman-cli-native-0.8.0-x86_64-pc-windows-msvc.zip" &&
                        downloadInfo.archiveType == "zip"
                }
            }

            should("return download info for linuxarm64 platform") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "linuxarm64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl ==
                        "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                        "v0.7.4/sdkman-cli-native-0.7.4-aarch64-unknown-linux-gnu.zip" &&
                        downloadInfo.archiveType == "zip"
                }
            }

            should("return download info for linuxx32 platform") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "linuxx32")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl ==
                        "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                        "v0.7.4/sdkman-cli-native-0.7.4-i686-unknown-linux-gnu.zip" &&
                        downloadInfo.archiveType == "zip"
                }
            }

            should("return download info for darwinx64 platform") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "darwinx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl ==
                        "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                        "v0.7.4/sdkman-cli-native-0.7.4-x86_64-apple-darwin.zip" &&
                        downloadInfo.archiveType == "zip"
                }
            }

            should("return download info for install command") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "linuxx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl.isNotBlank()
                }
            }

            should("return download info for selfupdate command") {
                val result = underTest.downloadNativeCli("selfupdate", "0.7.4", "linuxx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl.isNotBlank()
                }
            }
        }

        context("with invalid command") {
            should("return InvalidCommand error for unknown command") {
                val result = underTest.downloadNativeCli("invalid", "0.7.4", "linuxx64")

                result shouldBeLeft VersionError.InvalidCommand("invalid")
            }

            should("return InvalidCommand error for empty command") {
                val result = underTest.downloadNativeCli("", "0.7.4", "linuxx64")

                result shouldBeLeft VersionError.InvalidCommand("[empty/blank]")
            }

            should("return InvalidCommand error for case-sensitive command mismatch") {
                val result = underTest.downloadNativeCli("INSTALL", "0.7.4", "linuxx64")

                result shouldBeLeft VersionError.InvalidCommand("INSTALL")
            }

            should("return InvalidCommand error for update command (not supported)") {
                val result = underTest.downloadNativeCli("update", "0.7.4", "linuxx64")

                result shouldBeLeft VersionError.InvalidCommand("update")
            }
        }

        context("with invalid version") {
            should("return InvalidVersion error for empty version") {
                val result = underTest.downloadNativeCli("install", "", "linuxx64")

                result shouldBeLeft VersionError.InvalidVersion("[empty/blank]")
            }

            should("return InvalidVersion error for blank version") {
                val result = underTest.downloadNativeCli("install", "   ", "linuxx64")

                result shouldBeLeft VersionError.InvalidVersion("[empty/blank]")
            }
        }

        context("with invalid platform") {
            should("return InvalidPlatform error for unknown platform") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "invalidplatform")

                result shouldBeLeft VersionError.InvalidPlatform("invalidplatform")
            }

            should("return InvalidPlatform error for empty platform") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "")

                result shouldBeLeft VersionError.InvalidPlatform("[empty/blank]")
            }

            should("return InvalidPlatform error for exotic platform") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "exotic")

                result shouldBeLeft VersionError.InvalidPlatform("exotic")
            }

            should("return InvalidPlatform error for linuxarm32hf platform") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "linuxarm32hf")

                result shouldBeLeft VersionError.InvalidPlatform("linuxarm32hf")
            }

            should("return InvalidPlatform error for linuxarm32sf platform") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "linuxarm32sf")

                result shouldBeLeft VersionError.InvalidPlatform("linuxarm32sf")
            }

            should("return InvalidPlatform error for universal platform") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "universal")

                result shouldBeLeft VersionError.InvalidPlatform("universal")
            }
        }

        context("URL construction") {
            should("construct correct URL format with version prefix v") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "linuxx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl.startsWith(
                        "https://github.com/sdkman/sdkman-cli-native/releases/download/v0.7.4/"
                    )
                }
            }

            should("construct correct filename format") {
                val result = underTest.downloadNativeCli("install", "1.0.0", "windowsx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl.endsWith(
                        "sdkman-cli-native-1.0.0-x86_64-pc-windows-msvc.zip"
                    )
                }
            }

            should("include target triple in URL") {
                val result = underTest.downloadNativeCli("selfupdate", "0.7.4", "darwinarm64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl.contains("aarch64-apple-darwin")
                }
            }
        }

        context("Target triple mapping") {
            should("map linuxx64 to x86_64-unknown-linux-gnu") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "linuxx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl.contains("x86_64-unknown-linux-gnu")
                }
            }

            should("map linuxarm64 to aarch64-unknown-linux-gnu") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "linuxarm64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl.contains("aarch64-unknown-linux-gnu")
                }
            }

            should("map linuxx32 to i686-unknown-linux-gnu") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "linuxx32")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl.contains("i686-unknown-linux-gnu")
                }
            }

            should("map darwinx64 to x86_64-apple-darwin") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "darwinx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl.contains("x86_64-apple-darwin")
                }
            }

            should("map darwinarm64 to aarch64-apple-darwin") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "darwinarm64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl.contains("aarch64-apple-darwin")
                }
            }

            should("map windowsx64 to x86_64-pc-windows-msvc") {
                val result = underTest.downloadNativeCli("install", "0.7.4", "windowsx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.downloadUrl.contains("x86_64-pc-windows-msvc")
                }
            }

            should("handle all supported platforms correctly") {
                val platformTargetMap =
                    mapOf(
                        "linuxx64" to "x86_64-unknown-linux-gnu",
                        "linuxarm64" to "aarch64-unknown-linux-gnu",
                        "linuxx32" to "i686-unknown-linux-gnu",
                        "darwinx64" to "x86_64-apple-darwin",
                        "darwinarm64" to "aarch64-apple-darwin",
                        "windowsx64" to "x86_64-pc-windows-msvc"
                    )

                platformTargetMap.forEach { (platform, targetTriple) ->
                    val result = underTest.downloadNativeCli("install", "0.7.4", platform)

                    result shouldBeRightAnd { downloadInfo ->
                        downloadInfo.downloadUrl.contains(targetTriple) &&
                            downloadInfo.archiveType == "zip"
                    }
                }
            }
        }
    }
})
