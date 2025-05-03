package io.sdkman.broker

import io.kotest.common.ExperimentalKotest
import io.kotest.core.config.AbstractProjectConfig

@OptIn(ExperimentalKotest::class)
class KotestConfig : AbstractProjectConfig() {
    override val parallelism = 1
}
