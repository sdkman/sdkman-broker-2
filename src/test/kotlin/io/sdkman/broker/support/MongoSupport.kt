package io.sdkman.broker.support

import io.sdkman.broker.domain.model.Version
import org.bson.Document

object MongoSupport {
    fun setupVersion(version: Version) {
        val versionsCollection = MongoTestListener.database.getCollection("versions")
        versionsCollection.insertOne(
            Document().apply {
                put("candidate", version.candidate)
                put("version", version.version)
                put("platform", version.platform)
                put("url", version.url)
                version.vendor.map { put("vendor", it) }
                put("visible", version.visible)
                if (version.checksums.isNotEmpty()) {
                    put("checksums", Document(version.checksums))
                }
            }
        )
    }
}