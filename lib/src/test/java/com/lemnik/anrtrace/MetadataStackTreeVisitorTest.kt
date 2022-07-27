package com.lemnik.anrtrace

import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataStackTreeVisitorTest {

    private val visitor = MetadataStackTreeVisitor(ClassNameFormatter)

    @Test
    fun testStackTree() {
        val root = hashMapOf<String, Any>()
        val main = visitor.openBranch("com.android.internal.os.RuntimeInit", "main", 1, 3123000, root)
        val run = visitor.openBranch("java.lang.Thread", "run", 1, 423000, main)

        visitor.visitLeaf("com.lemnik.FakeClass", "testMethod", 10, 10, run)
        visitor.visitLeaf("com.lemnik.FakeClass", "testMethod2", 10, 82300, run)

        visitor.closeBranch(run, main)
        visitor.closeBranch(main, root)

        assertEquals(
            mapOf(
                "com.android.internal.os.RuntimeInit.main [1 3ms]" to mapOf(
                    "java.lang.Thread.run [1 .4ms]" to mapOf(
                        "com.lemnik.FakeClass.testMethod" to "10 10ns",
                        "com.lemnik.FakeClass.testMethod2" to "10 .08ms",
                    )
                )
            ),
            root
        )
    }
}