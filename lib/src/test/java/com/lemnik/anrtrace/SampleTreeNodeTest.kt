package com.lemnik.anrtrace

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class SampleTreeNodeTest {
    @Test
    fun testSimpleChildStructure() {
        val root = SampleTreeNode("", "")
        val child1 = root.child("SampleTreeNodeTest", "testChildStructure")
        val child2 = child1.child("SampleTreeNodeTest", "hashCode")
        val child3 = child2.child("SampleTreeNodeTest", "hashCode")
        val child1_ = root.child("SampleTreeNodeTest", "testChildStructure")
        val child2_ = child1.child("SampleTreeNodeTest", "hashCode")
        val child3_ = child2.child("SampleTreeNodeTest", "hashCode")
        Assert.assertSame(child1, child1_)
        Assert.assertSame(child2, child2_)
        Assert.assertSame(child3, child3_)
    }

    @Test
    fun testStackTree() {
        val root = SampleTreeNode("", "")

        var fileNumber = 0
        do {
            val trace =
                SampleTreeNodeTest::class.java.getResourceAsStream("/stack_trace_$fileNumber.txt")
                    ?.reader()
                    ?.readLines()
                    ?.reversed()

            fileNumber++
            if (trace != null) {
                var frame = root
                trace.forEach { line ->
                    frame = frame.child(
                        line.substringBefore(':').intern(),
                        line.substringAfter(':').intern()
                    )

                    frame.counter++
                }
            } else {
                fileNumber = -1
            }
        } while (fileNumber >= 0)

        val expected =
            SampleTreeNodeTest::class.java.getResourceAsStream("/expected_counters.txt")!!
                .reader()
                .readLines()

        val stack = ArrayList<SampleTreeNode>()
        stack.add(root)
        expected.forEach { line ->
            val expectedStackDepth =
                line.substringBefore(' ', missingDelimiterValue = "").trim().length
            while (expectedStackDepth < stack.size - 1) {
                stack.removeLast()
            }

            val expectation = line.substringAfter(' ').trim()
            val frame = expectation.substringBefore('=').trim()
            val expectedCount = expectation.substringAfter('=').trim().toLong()

            val node = stack.last().child(
                frame.substringBefore(':').intern(),
                frame.substringAfter(':').intern(),
            )

            assertEquals(
                stack.joinToString("\n"),
                expectedCount,
                node.counter
            )

            stack.add(node)
        }
    }
}