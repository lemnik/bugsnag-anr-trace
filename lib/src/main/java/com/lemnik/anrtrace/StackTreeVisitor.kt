package com.lemnik.anrtrace

/**
 * Allows customisation of the performance data output. When an ANR is being reported, this class
 * is used to convert the internal performance tree into a representation that can be added
 * to a Bugsnag `Event` object.
 *
 * Implementations of this interface should aim to be relatively quick as they are invoked from
 * within a Bugsnag `OnErrorCallback`.
 *
 * *Note*: The order that the stack frames are visited is not defined beyond them being
 * depth-first. Any ordering will need to be done in the `StackTreeVisitor` implementation.
 */
interface StackTreeVisitor<E> {
    /**
     * Begin a single graph walk by generating a root token (if applicable). The returned token
     * will be passed to [openBranch] for each of the direct descendants of the root node (the
     * bottom of the stack tree, typically a `Thread.run` or similar call), and then to [end].
     */
    fun begin(): E

    /**
     * End a single graph walk and attach the final data to the given metadata object. [token] is the
     * value returned from the corresponding [begin] call.
     */
    fun end(metadata: MutableMap<String, Any>, token: E)

    fun openBranch(
        className: String,
        methodName: String,
        callCount: Long,
        estimatedTimeNs: Long,
        parent: E
    ): E

    fun closeBranch(node: E, parent: E) = Unit

    fun visitLeaf(
        className: String,
        methodName: String,
        callCount: Long,
        estimatedTimeNs: Long,
        parent: E
    )
}