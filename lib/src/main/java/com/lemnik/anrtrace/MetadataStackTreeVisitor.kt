package com.lemnik.anrtrace

/**
 * An extremely verbose, but pretty [StackTreeVisitor] that is only really suitable for debugging.
 * Each branch element of the stack tree is built as a Metadata `Map` and each leaf is represented
 * by a human-readable string.
 */
class MetadataStackTreeVisitor constructor(
    /**
     * How to format the class names that appear in the metadata. By default the fully-qualified
     * class name is used with no trimming.
     *
     * @see ClassNameFormatter.ShortPackageName
     * @see ClassNameFormatter.SimpleName
     */
    private val classNameFormat: ClassNameFormatter = ClassNameFormatter,
) : StackTreeVisitor<MutableMap<String, Any>> {
    override fun begin(): MutableMap<String, Any> {
        return hashMapOf()
    }

    override fun end(metadata: MutableMap<String, Any>, token: MutableMap<String, Any>) {
        metadata.putAll(token)
    }

    override fun openBranch(
        className: String,
        methodName: String,
        callCount: Long,
        estimatedTimeNs: Long,
        parent: MutableMap<String, Any>
    ): MutableMap<String, Any> {
        val childNode = hashMapOf<String, Any>()
        parent[formatNode(className, methodName, callCount, estimatedTimeNs)] = childNode
        return childNode
    }

    private fun formatNode(
        className: String,
        methodName: String,
        counter: Long,
        estimatedTimeNs: Long
    ) = buildString {
        classNameFormat(this, className)
        append('.').append(methodName)
            .append(' ').append('[')
            .append(counter)
            .append(' ')
        humanFormatTimeNs(estimatedTimeNs)

        append(']')
    }

    override fun closeBranch(node: MutableMap<String, Any>, parent: MutableMap<String, Any>) = Unit

    override fun visitLeaf(
        className: String,
        methodName: String,
        callCount: Long,
        estimatedTimeNs: Long,
        parent: MutableMap<String, Any>
    ) {
        val nodeName = buildString {
            classNameFormat(this, className)
            append('.').append(methodName)
        }

        parent[nodeName] = buildString {
            append(callCount).append(' ')
            humanFormatTimeNs(estimatedTimeNs)
        }
    }
}