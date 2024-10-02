package org.plan.research.fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.collections.immutable.toPersistentHashMap
import org.plan.research.fuzz.templates.persistentMapRandomOps
import org.plan.research.fuzz.utils.validateBuilder
import org.plan.research.fuzz.utils.validateInvariants
import org.plan.research.fuzz.utils.validateReplay
import org.plan.research.fuzz.utils.validateReverse

class PersistentHashMapTests {
    @FuzzTest(maxDuration = "2h")
    fun randomOpsValidateInvariants(data: FuzzedDataProvider) {
        validateInvariants = true
        persistentMapRandomOps(data) { toPersistentHashMap() }
    }

    @FuzzTest(maxDuration = "2h")
    fun randomOpsValidateReverse(data: FuzzedDataProvider) {
        validateReverse = true
        persistentMapRandomOps(data) { toPersistentHashMap() }
    }

    @FuzzTest(maxDuration = "2h")
    fun randomOpsValidateReplay(data: FuzzedDataProvider) {
        validateReplay = true
        persistentMapRandomOps(data) { toPersistentHashMap() }
    }

    @FuzzTest(maxDuration = "2h")
    fun randomOpsValidateBuilder(data: FuzzedDataProvider) {
        validateBuilder = true
        persistentMapRandomOps(data) { toPersistentHashMap() }
    }
}