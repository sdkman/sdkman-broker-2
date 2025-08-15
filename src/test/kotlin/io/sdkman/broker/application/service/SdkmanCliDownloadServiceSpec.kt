package io.sdkman.broker.application.service

import io.kotest.core.spec.style.ShouldSpec
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.support.shouldBeLeft
import io.sdkman.broker.support.shouldBeRightAnd

class SdkmanCliDownloadServiceSpec : ShouldSpec({
    val underTest = SdkmanCliDownloadServiceImpl()

    context("downloadSdkmanCli") {
        context("with valid inputs") {
            should("return download info for stable version with install command") {
                val result = underTest.downloadSdkmanCli("install", "5.19.0", "linuxx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.redirectUrl ==
                        "https://github.com/sdkman/sdkman-cli/releases/download/" +
                        "5.19.0/sdkman-cli-5.19.0.zip" &&
                        downloadInfo.archiveType == "zip"
                }
            }

            should("return download info for stable version with selfupdate command") {
                val result = underTest.downloadSdkmanCli("selfupdate", "5.19.0", "darwinarm64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.redirectUrl ==
                        "https://github.com/sdkman/sdkman-cli/releases/download/" +
                        "5.19.0/sdkman-cli-5.19.0.zip" &&
                        downloadInfo.archiveType == "zip"
                }
            }

            should("return download info for beta version with latest+ prefix") {
                val result = underTest.downloadSdkmanCli("install", "latest+b8d230b", "linuxx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.redirectUrl == "https://github.com/sdkman/sdkman-cli/releases/download/" +
                        "latest/sdkman-cli-latest+b8d230b.zip" &&
                        downloadInfo.archiveType == "zip"
                }
            }

            should("return download info for different platform (platform not used in URL construction)") {
                val result = underTest.downloadSdkmanCli("install", "5.19.0", "windowsx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.redirectUrl == "https://github.com/sdkman/sdkman-cli/releases/download/" +
                        "5.19.0/sdkman-cli-5.19.0.zip" &&
                        downloadInfo.archiveType == "zip"
                }
            }
        }

        context("with invalid command") {
            should("return InvalidCommand error for unknown command") {
                val result = underTest.downloadSdkmanCli("invalid", "5.19.0", "linuxx64")

                result shouldBeLeft VersionError.InvalidCommand("invalid")
            }

            should("return InvalidCommand error for empty command") {
                val result = underTest.downloadSdkmanCli("", "5.19.0", "linuxx64")

                result shouldBeLeft VersionError.InvalidCommand("")
            }

            should("return InvalidCommand error for case-sensitive command mismatch") {
                val result = underTest.downloadSdkmanCli("INSTALL", "5.19.0", "linuxx64")

                result shouldBeLeft VersionError.InvalidCommand("INSTALL")
            }
        }

        context("with invalid version") {
            should("return InvalidVersion error for empty version") {
                val result = underTest.downloadSdkmanCli("install", "", "linuxx64")

                result shouldBeLeft VersionError.InvalidVersion("[empty/blank]")
            }

            should("return InvalidVersion error for blank version") {
                val result = underTest.downloadSdkmanCli("install", "   ", "linuxx64")

                result shouldBeLeft VersionError.InvalidVersion("[empty/blank]")
            }
        }

        context("with invalid platform") {
            should("return InvalidPlatform error for unknown platform") {
                val result = underTest.downloadSdkmanCli("install", "5.19.0", "invalidplatform")

                result shouldBeLeft VersionError.InvalidPlatform("invalidplatform")
            }

            should("return InvalidPlatform error for empty platform") {
                val result = underTest.downloadSdkmanCli("install", "5.19.0", "")

                result shouldBeLeft VersionError.InvalidPlatform("[empty/blank]")
            }
        }

        context("URL construction") {
            should("construct correct URL for semantic version") {
                val result = underTest.downloadSdkmanCli("install", "5.19.0", "linuxx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.redirectUrl ==
                        "https://github.com/sdkman/sdkman-cli/releases/download/" +
                        "5.19.0/sdkman-cli-5.19.0.zip"
                }
            }

            should("construct correct URL for beta version with commit hash") {
                val result = underTest.downloadSdkmanCli("install", "latest+a1b2c3d", "linuxx64")

                result shouldBeRightAnd { downloadInfo ->
                    downloadInfo.redirectUrl ==
                        "https://github.com/sdkman/sdkman-cli/releases/download/" +
                        "latest/sdkman-cli-latest+a1b2c3d.zip"
                }
            }
        }
    }
})
