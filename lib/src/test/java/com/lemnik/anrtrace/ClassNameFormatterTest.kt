package com.lemnik.anrtrace

import org.junit.Assert.assertEquals
import org.junit.Test

class ClassNameFormatterTest {
    @Test
    fun fullClassNameFormatter() {
        val thread = buildString { ClassNameFormatter(this, "java.lang.Thread") }
        val noPackageClass = buildString { ClassNameFormatter(this, "NoPackageClass") }
        assertEquals("java.lang.Thread", thread)
        assertEquals("NoPackageClass", noPackageClass)
    }

    @Test
    fun simpleClassNameFormatter() {
        val formatter = ClassNameFormatter.SimpleName
        val thread = buildString { formatter(this, "java.lang.Thread") }
        val innerClass = buildString { formatter(this, "java.lang.Thread\$State") }
        val noPackageClass = buildString { formatter(this, "NoPackageClass") }
        assertEquals("Thread", thread)
        assertEquals("Thread\$State", innerClass)
        assertEquals("NoPackageClass", noPackageClass)
    }

    @Test
    fun shortPackageFormatter() {
        val formatter = ClassNameFormatter.ShortPackageName
        val thread = buildString { formatter(this, "java.lang.Thread") }
        val looper = buildString { formatter(this, "android.os.Looper") }
        val innerClass = buildString { formatter(this, "java.lang.Thread\$State") }
        val noPackageClass = buildString { formatter(this, "NoPackageClass") }
        assertEquals("j.l.Thread", thread)
        assertEquals("a.o.Looper", looper)
        assertEquals("j.l.Thread\$State", innerClass)
        assertEquals("NoPackageClass", noPackageClass)
    }
}