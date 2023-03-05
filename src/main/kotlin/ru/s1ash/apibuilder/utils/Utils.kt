package ru.s1ash.apibuilder.utils

fun Array<String>.toArgumentsMap(keyPrefix: String = "--"): Map<String, String> {
    val map = mutableMapOf<String, String>()
    var argumentName: String? = null
    forEach { arg ->
        if (arg.startsWith(keyPrefix)) {
            argumentName = argumentName.let {
                if (it != null) map[it] = "true"
                arg.substring(keyPrefix.length)
            }
            return@forEach
        }
        argumentName?.let {
            map[it] = arg
            argumentName = null
        }
    }
    argumentName?.let { map[it] = "true" }
    return map.toMap()
}