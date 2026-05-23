package io.sdkman.broker

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.engine.concurrency.SpecExecutionMode

class KotestConfig : AbstractProjectConfig() {
    override val specExecutionMode = SpecExecutionMode.Sequential
}
