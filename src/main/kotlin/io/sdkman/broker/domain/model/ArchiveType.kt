package io.sdkman.broker.domain.model

sealed class ArchiveType(val value: String) {
    data object Zip : ArchiveType("zip")

    data object TarGz : ArchiveType("tar.gz")

    companion object {
        fun fromUrl(url: String): ArchiveType =
            when {
                url.endsWith(".zip") -> Zip
                url.endsWith(".tar.gz") || url.endsWith(".tgz") -> TarGz
                else -> Zip // Default fallback
            }
    }
}
