package org.plan.research

import kotlinx.collections.immutable.persistentSetOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Reproduce {
    @Test
    fun persistentOrderedSetNotEqualsBuilder(){
        val set1 = persistentSetOf(-486539264, 16777216, 0, 67108864)
        val builder = set1.builder()

        assertEquals(set1, builder)
        assertEquals(set1, builder.build())
        assertEquals(set1, builder.build().toSet())


        val set2 = set1.remove(0)
        builder.remove(0)

        assertEquals(set2, builder.build().toSet())
        assertEquals(set2, builder.build())
    }

    @Test
    fun persistentOrderedSetNotEqualsBuildera(){
        val set1 = persistentSetOf(-1, 2, 0, 3)
        val builder = set1.builder()

        assertEquals(set1, builder)
        assertEquals(set1, builder.build())
        assertEquals(set1, builder.build().toSet())


        val set2 = set1.remove(0)
        builder.remove(0)

        assertEquals(set2, builder.build().toSet())
        assertEquals(set2, builder.build())
    }
}
