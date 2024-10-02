/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("USELESS_IS_CHECK", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.plan.research.fuzz.other

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import kotlinx.collections.immutable.*
import org.junit.jupiter.api.Assertions.assertTrue

class UselessTests {

    companion object {
        inline fun ignoreExceptions(block: () -> Unit) = try {
            block()
        } catch (e: AssertionError) {
            throw e
        } catch (e: Throwable) {
            System.err.println("#crash" + System.currentTimeMillis())
        }
    }

    @FuzzTest(maxDuration = "30s")
    fun testEmpty(list: PersistentList<*>?) {
        if (list == null) return

        ignoreExceptions {
            assertTrue(list.mutate { it.clear() }.isEmpty())
        }
    }


    @FuzzTest(maxDuration = "30s")
    fun test(a: Int) {
        if (a * (1e9 + 7).toInt() + 31 == 42) {
            throw IllegalArgumentException()
        }
    }

    @FuzzTest(maxDuration = "30s")
    fun a(list: List<Int>?) {
        if (list === null) return
        val persistent = list.toPersistentList()
        ignoreExceptions { assertTrue(persistent.mutate { it.clear() }.isEmpty()) }
    }

    @FuzzTest
    fun inner(a: List<List<Int>>?): Unit = ignoreExceptions {
        if (a == null) return
        assertTrue(a.all { it is List<Int> })
        val list = a.map { it.toPersistentList() }.toPersistentList()
        assertTrue(list.mutate { it.clear() }.isEmpty())
        list.forEachIndexed { index, ilist ->
            assertTrue(ilist.toList() == a[index])
        }
    }

    @FuzzTest
    fun innerArrayList(a: ArrayList<ArrayList<Int>>?): Unit = ignoreExceptions {
        if (a == null) return
        assertTrue(a.all { it is List<Int> })
        val list = a.map { it.toPersistentList() }.toPersistentList()
        assertTrue(list.mutate { it.clear() }.isEmpty())
        list.forEachIndexed { index, ilist ->
            assertTrue(ilist.toList() == a[index])
        }
    }

    @FuzzTest(maxDuration = "30s")
    fun persistentMap(persistentMap: PersistentMap<Int, Int>?) = ignoreExceptions {
        if (persistentMap == null) return
        for (key in persistentMap.keys) {
            assertTrue(persistentMap.containsKey(key))
        }
    }

    @FuzzTest(maxDuration = "30s")
    fun map2Persistent(map: Map<String, String>?) {
        if (map == null) return
        val persistent = map.toPersistentMap()
        assertTrue(map.entries.all { (k, v) -> persistent[k] == v })
    }

    @FuzzTest(maxDuration = "30s")
    fun mapEmptyString(map: Map<String, String>?) {
        if (map == null) return
        val persistent = map.toPersistentMap()
        assertTrue(persistent.mutate { it.clear() }.isEmpty())
    }

    @FuzzTest(maxDuration = "30s")
    fun mapEmptyInt(map: Map<Int, Int>?) {
        if (map == null) return
        val persistent = map.toPersistentMap()
        assertTrue(persistent.mutate { it.clear() }.isEmpty())
    }


    @FuzzTest(maxDuration = "30s")
    fun map2PersistentProvider(data: FuzzedDataProvider) {
        val size = data.consumeInt(0, 1000)
        val list = List(size) { data.consumeString(10) }
        val persistent = list.toPersistentList()
        list.forEachIndexed { index, elem -> assertTrue(elem == persistent[index]) }
    }

    @FuzzTest(maxDuration = "30m")
    fun wtf(list: List<Int>?) = ignoreExceptions {
        if (list == null) return
        val chunked = list.chunked(10)
        val persistent = chunked.map { it.toPersistentList() }.toPersistentList()
        persistent.mutate { it.map { it.mutate { it.addAll(persistent.flatten().toList()) } } }
    }

//    @FuzzTest(maxDuration = "30m")
//    fun lol(@NotNull list: List<Int>?) = ignoreExceptions {
//        if (list == null) return
//        val builder = list.toPersistentList().builder()
//        val chunked = builder.chunked(10)
//        assertTrue(chunked.isNotEmpty() == list.isNotEmpty())
//    }

    @FuzzTest(maxDuration = "30m")
    fun lolPlatform(list: List<Int>?) = ignoreExceptions {
        if (list == null) return
        val builder = list.toPersistentList().builder()
        val chunked = builder.chunked(10)
        assertTrue(chunked.isNotEmpty() == list.isNotEmpty())
    }

    @FuzzTest(maxDuration = "30m")
    fun lolArray(list: IntArray?) = ignoreExceptions {
        if (list == null) return
        val builder = list.toTypedArray().toPersistentList()
        val chunked = builder.chunked(10)
        assertTrue(chunked.isNotEmpty() == list.isNotEmpty())
    }

    @FuzzTest(maxDuration = "60s")
    fun bubbleSort(list: IntArray?) = ignoreExceptions {
        if (list == null) {
            System.err.println("got null")
            return
        }
        System.err.println("not null")
        val steps: MutableList<PersistentList<Int>> =
            mutableListOf(list.toTypedArray().toPersistentList())
        var current = steps.first()
        var done = false
        while (!done) {
            done = true
            for (i in 0..<list.lastIndex) {
                if (current[i] > current[i + 1]) {
                    done = false
                    current = current.mutate {
                        it[i] = current[i + 1]
                        it[i + 1] = current[i]
                    }
                    steps.add(current)
                }
            }
        }

        val mutable = list.toMutableList()
        done = false

        val iter = steps.iterator()
        assertTrue(mutable.toList() == iter.next())

        while (!done) {
            done = true
            for (i in 0..<list.lastIndex) {
                if (mutable[i] > mutable[i + 1]) {
                    done = false
                    val t = mutable[i]
                    mutable[i] = mutable[i + 1]
                    mutable[i + 1] = t

                    assertTrue(mutable.toList() == iter.next())
                }
            }
        }
    }

    @FuzzTest(maxDuration = "60s")
    fun ahahah(array: Array<*>?) = ignoreExceptions {
        if (array == null) {
            System.err.println("got null")
            return
        }
        System.err.println("not null")
        var list = array.toPersistentList()
        list = list.add(list)
        list = list.add(list)
        list = list.add(list)
        list = list.add(list)
        assertTrue(list.size == array.size + 4)
    }


    @FuzzTest(maxDuration = "30s")
    fun randomOps(data: FuzzedDataProvider) {
        val operationsNumber = data.consumeInt(0, 100)
        val current = persistentSetOf<Int>().builder()
        val previous = mutableListOf(current.build())
        val realSet = mutableSetOf<Int>()
        repeat(operationsNumber) {
            when (data.consumeInt(0, 1)) {
                0 -> {
                    val n = data.consumeInt()
                    current.remove(n)
                    realSet.remove(n)
                }

                1 -> {
                    val n = data.consumeInt()
                    current.add(n)
                    realSet.add(n)
                }
            }
            previous.add(current.build())
            assertTrue(realSet == current)
        }
    }


    data class EvenInt(val value: Int) {
        init {
            require(value % 2 == 0)
        }
    }

    data class HardInt(val value: Int) {
        init {
            require(value % 2 == 0)
            require(value % 3 == 0)
            require(value % 5 == 0)
        }
    }

    data class PrimeInt(val value: Int) {
        init {
            require(value in 2..99)
            var k = 2
            while (k * k <= value) {
                require(value % k != 0)
                k++
            }
        }
    }

    @FuzzTest(maxDuration = "30m")
    fun persistentHashSetIsPersistentData(data: FuzzedDataProvider) {
        val elems = data.consumeString(1000).chunked(10)
        val set = elems.toSet()
        val mutSet = elems.toMutableSet()
        val persistent = set.toPersistentSet()
        val current = persistent.builder()
        repeat(1000) {
            val n = data.consumeString(10)
            val add = data.consumeBoolean()
            if (add) {
                current.add(n)
                mutSet.add(n)
            } else {
                current.remove(n)
                mutSet.remove(n)
            }
            assertTrue(set == persistent)
            assertTrue(mutSet == current)
        }
    }


    @FuzzTest(maxDuration = "30m")
    fun nullTest(eventInt: EvenInt?) {
        if (eventInt == null) {
            System.err.println("got null")
            return
        }
        System.err.println("not null")
        assertTrue(eventInt.value % 2 == 0)
    }

    @FuzzTest(maxDuration = "30m")
    fun nullTestAnnotation(eventInt: EvenInt?) {
        if (eventInt == null) {
            System.err.println("got null")
            return
        }
        System.err.println("not null")
        assertTrue(eventInt.value % 2 == 0)
    }

    @FuzzTest(maxDuration = "60s")
    fun testPrime(primeInt: PrimeInt?) {
        if (primeInt == null) {
            System.err.println("got null")
            return
        }
        System.err.println("not null")
        assertTrue(primeInt.value != 0)
    }

    @FuzzTest(maxDuration = "60s")
    fun testHardInt(hardInt: HardInt?) {
        if (hardInt == null) {
            System.err.println("got null")
            return
        }
        System.err.println("not null")
        val value = hardInt.value
        require(value % 2 == 0)
        require(value % 3 == 0)
        require(value % 5 == 0)
    }

    data class MyInt(val value: Int)

    @FuzzTest(maxDuration = "60s")
    fun boxedInt(boxed: Integer?) {
        if (boxed == null) {
            System.err.println("got null")
            return
        }
        System.err.println("not null")
        assertTrue(boxed.toInt() == 0 || boxed.toInt() != 0)
    }

    @FuzzTest(maxDuration = "60s")
    fun myBoxedInt(boxed: MyInt?){
        if (boxed == null) {
            System.err.println("got null")
            return
        }
        System.err.println("not null")
        assertTrue(boxed.value == 0 || boxed.value != 0)
    }

    @FuzzTest(maxDuration = "60s")
    fun nullTest2(a1: MyInt?, a2: MyInt?){
        if (a1 == null || a2 == null) {
            System.err.println("got null")
            return
        }
        System.err.println("not null")
        assertTrue(a1 == a2 || a1 != a2)
    }

    @FuzzTest(maxDuration = "60s")
    fun autoArray(array: IntArray?){
        if (array == null) return
        assertTrue(array.toTypedArray().toPersistentList().size == array.size)
    }

    @FuzzTest(maxDuration = "60s")
    fun fuzzArray(data: FuzzedDataProvider) {
        val array = data.consumeInts(1000)
        assertTrue(array.toTypedArray().toPersistentList().size == array.size)
    }


//    @FuzzTest(maxDuration = "60s")
//    fun leetcode(data: FuzzedDataProvider){
//
//    }
}
