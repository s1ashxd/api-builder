package ru.s1ash.apibuilder.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@JvmField
val APP_LOGGER: Logger = LoggerFactory.getLogger("API Builder")

@OptIn(ExperimentalContracts::class)
inline fun <T> measureTime(placeholder: String, block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val startTime = System.currentTimeMillis()
    val result = block()
    APP_LOGGER.info(placeholder, System.currentTimeMillis() - startTime)
    return result
}

@OptIn(ExperimentalContracts::class)
inline fun log(block: Logger.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    APP_LOGGER.block()
}