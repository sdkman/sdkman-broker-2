package io.sdkman.broker.domain.model

sealed class ArchiveType(
    val value: String
) {
    data object Zip : ArchiveType("zip")

    data object TarGz : ArchiveType("tar.gz")

    data object TarBz2 : ArchiveType("tar.bz2")

    data object Xz : ArchiveType("xz")

    companion object {
        fun fromUrl(url: String): ArchiveType =
            when {
                url.endsWith(".zip") -> Zip
                url.endsWith(".tar.gz") || url.endsWith(".tgz") -> TarGz
                url.endsWith(".tar.bz2") -> TarBz2
                url.endsWith(".xz") -> Xz
                else -> Zip
            }
    }
}
