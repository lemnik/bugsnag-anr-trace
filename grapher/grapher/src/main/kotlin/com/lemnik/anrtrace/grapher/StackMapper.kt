package com.lemnik.anrtrace.grapher

data class MethodInfo(
    val className: String,
    val methodName: String,
    val isLeaf: Boolean,
)

interface StackMapper {
    fun unmap(className: String, methodName: String): Collection<MethodInfo>
}

object DefaultStackMapper : StackMapper {
    override fun unmap(className: String, methodName: String): Collection<MethodInfo> {
        return listOf(MethodInfo(className, methodName, false))
    }
}