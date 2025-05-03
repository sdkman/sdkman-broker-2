package io.sdkman.broker

import io.kotest.common.ExperimentalKotest
import io.kotest.core.config.AbstractProjectConfig

// Disables parallel execution of tests to prevent container conflicts
@OptIn(ExperimentalKotest::class)
class KotestConfig : AbstractProjectConfig() {
    override val parallelism = 1
}
