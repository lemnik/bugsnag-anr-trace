package com.lemnik.anrtrace

import java.util.*

/**
 * [StackTreeVisitor] implementation that only reports the hottest (most commonly seen) stack path.
 * Useful if your application is likely to be blocked by a single method (ie: if you do file IO or
 * use lock-conditions on the `main` thread)
 */
class HotStackTreeVisitor @JvmOverloads constructor(
    /**
     * How to format the class names that appear in the metadata. By default the fully-qualified
     * class name is used with no trimming.
     */
    private val classNameFormat: ClassNameFormatter = ClassNameFormatter,
) : StackTreeVisitor<HotStackTreeVisitor.HotStackTraceFrame?> {

    override fun begin(): HotStackTraceFrame {
        return HotStackTraceFrame("", 0L, 0L)
    }

    override fun end(token: HotStackTraceFrame?, addMetadata: (String, Any) -> Unit) {
        addMetadata(
            "Hot Stack Trace",
            LinkedList<String>().apply {
                var frame = token?.child

                while (frame != null) {
                    add(frame.toString())
                    frame = frame.child
                }
            },
        )
    }

    override fun openBranch(
        className: String,
        methodName: String,
        lineNumber: Int,
        callCount: Long,
        estimatedTimeNs: Long,
        parent: HotStackTraceFrame?
    ): HotStackTraceFrame? {
        if (parent == null) return null
        val lChild = parent.child

        if (lChild == null || lChild.callCount < callCount) {
            parent.child = HotStackTraceFrame(
                buildNodeName(className, methodName, lineNumber.toString()),
                callCount,
                estimatedTimeNs,
                parent
            )

            return parent.child
        }

        return null
    }

    override fun visitLeaf(
        className: String,
        methodName: String,
        lineNumber: Int,
        callCount: Long,
        estimatedTimeNs: Long,
        parent: HotStackTraceFrame?
    ) {
        val lChild = parent?.child

        if (lChild == null || lChild.callCount < callCount) {
            parent?.child = HotStackTraceFrame(
                buildNodeName(className, methodName, lineNumber.toString()),
                callCount,
                estimatedTimeNs,
                parent
            )
        }
    }

    private fun buildNodeName(className: String, methodName: String, lineNumber: String) =
        StringBuilder(className.length + methodName.length + lineNumber.length + 2)
            .also { builder ->
                classNameFormat(builder, className)
                builder.append('.')
                    .append(methodName)
                    .append(':')
                    .append(lineNumber)
            }
            .toString()

    data class HotStackTraceFrame(
        var name: String,
        var callCount: Long,
        var estimatedTimeNs: Long,
        var parent: HotStackTraceFrame? = null,
        var child: HotStackTraceFrame? = null,
    ) {
        override fun toString(): String {
            return buildString {
                append(name)
                    .append(' ')
                    .append(callCount)
                    .append(' ')
                    .humanFormatTimeNs(estimatedTimeNs)
            }
        }
    }
}