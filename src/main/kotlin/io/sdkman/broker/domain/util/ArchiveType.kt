package io.sdkman.broker.domain.util

enum class ArchiveType(val extension: String) {
    ZIP("zip"),
    TAR_GZ("tar.gz"),
    TBZ2("tar.bz2"),
    XZ("tar.xz");

    companion object {
        fun fromUrl(url: String): ArchiveType = when {
            url.endsWith(".zip") -> ZIP
            url.endsWith(".tar.gz") || url.endsWith(".tgz") -> TAR_GZ
            url.endsWith(".tar.bz2") || url.endsWith(".tbz2") -> TBZ2
            url.endsWith(".tar.xz") || url.endsWith(".txz") -> XZ
            else -> ZIP // Default fallback
        }
    }
}