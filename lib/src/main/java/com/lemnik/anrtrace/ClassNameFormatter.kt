package com.lemnik.anrtrace

fun interface ClassNameFormatter : (StringBuilder, String) -> Unit {
    fun StringBuilder.formatClassName(fullClassName: String)

    override operator fun invoke(output: StringBuilder, fullClassName: String) {
        output.formatClassName(fullClassName)
    }

    companion object FullClassName : ClassNameFormatter {
        override fun StringBuilder.formatClassName(fullClassName: String) {
            append(fullClassName)
        }
    }

    /**
     * Convenient class name formatter that drops the package names completely from class name.
     * So `android.os.Looper` becomes `Looper`, and `com.android.internal.os.RuntimeInit`
     * becomes `RuntimeInit`.
     */
    object SimpleName : ClassNameFormatter {
        override fun StringBuilder.formatClassName(fullClassName: String) {
            // lastIndexOf returns -1 on "no match", -1 + 1 = 0, yay for maths!
            val classNameStart = fullClassName.lastIndexOf('.') + 1
            append(fullClassName, classNameStart, fullClassName.length)
        }
    }

    /**
     * Convenient class name formatter that trims the package names to a single character each
     * so that they can remain understood without bloating the size of the metadata.
     *
     * For example this transformation will convert `com.android.internal.os.RuntimeInit`
     * into `c.a.i.o.RuntimeInit`, and `android.os.Looper` into `a.o.Looper`.
     */
    object ShortPackageName : ClassNameFormatter {
        override fun StringBuilder.formatClassName(fullClassName: String) {
            var cursor = 0
            var dot = fullClassName.indexOf('.')

            // split and friends are expensive and we're already ANRed! - hand crank the string
            if (dot != -1) {
                while (dot != -1) {
                    append(fullClassName[cursor]).append('.')
                    cursor = dot + 1
                    dot = fullClassName.indexOf('.', cursor)
                }

                append(fullClassName, cursor, fullClassName.length)
            } else {
                append(fullClassName)
            }
        }

    }
}
