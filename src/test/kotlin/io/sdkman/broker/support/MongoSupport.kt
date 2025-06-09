package io.sdkman.broker.support

import io.sdkman.broker.domain.model.Version
import org.bson.Document

object MongoSupport {
    fun setupVersion(version: Version) {
        val versionsCollection = MongoTestListener.versionsCollection
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

    // Sets up a valid application record
    fun setupValidAppRecord() {
        val applicationCollection = MongoTestListener.applicationCollection
        applicationCollection.insertOne(Document("alive", "OK"))
    }

    // Sets up an invalid application record
    fun setupInvalidAppRecord() {
        val applicationCollection = MongoTestListener.applicationCollection
        applicationCollection.insertOne(Document("alive", "NOT_OK"))
    }
}
