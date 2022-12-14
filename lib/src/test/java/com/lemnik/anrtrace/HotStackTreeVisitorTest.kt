package com.lemnik.anrtrace

import org.junit.Assert
import org.junit.Test

class HotStackTreeVisitorTest {
    private val visitor = HotStackTreeVisitor(ClassNameFormatter)

    @Test
    fun testStackTree() {
        val root = visitor.begin()

        val main =
            visitor.openBranch("com.android.internal.os.RuntimeInit", "main", 1, 1, 3123000, root)
        val run = visitor.openBranch("java.lang.Thread", "run", 1, 5, 423000, main)

        visitor.visitLeaf("com.lemnik.FakeClass", "testMethod", 1, 10, 10, run)
        visitor.visitLeaf("com.lemnik.FakeClass", "testMethod2", 1, 12, 82300, run)

        visitor.closeBranch(run, main)

        visitor.visitLeaf("com.lemnik.FakeClass", "testMethod3", 1, 3, 543, main)

        visitor.closeBranch(main, root)

        val stack = HashMap<String, Any>().also { map -> visitor.end(root, map::put) }.values.first()

        Assert.assertEquals(
            listOf(
                "com.android.internal.os.RuntimeInit.main:1 1 3ms",
                "java.lang.Thread.run:1 5 .4ms",
                "com.lemnik.FakeClass.testMethod2:1 12 .08ms",
            ),
            stack
        )
    }
}