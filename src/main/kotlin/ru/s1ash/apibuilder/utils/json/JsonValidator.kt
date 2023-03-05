package ru.s1ash.apibuilder.utils.json

import com.google.gson.JsonElement
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import ru.s1ash.apibuilder.utils.APP_LOGGER
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class JsonValidator(
    private val json: JsonElement,
    val path: List<String>
) {

    fun asPrimitive(parent: String? = null, required: Boolean = true) =
        validate(parent, required, { isJsonPrimitive }) {}

    fun asObject(
        parent: String? = null,
        required: Boolean = true,
        validateChildren: JsonValidator.() -> Unit = {}
    ) = validate(parent, required, { isJsonObject }) { validateElement(parent, path) { validateChildren() } }

    fun asArray(
        parent: String? = null,
        required: Boolean = true,
        validateElement: JsonValidator.() -> Unit = {}
    ) = validate(parent, required, { isJsonArray }) {
        asJsonArray.forEach {
            it.validateElement(parent, path) { validateElement() }
        }
    }

    fun asEnum(parent: String? = null, required: Boolean = true, values: Array<out Enum<*>>) =
        validate(parent, required, { isJsonPrimitive && values.any { it.name == asString } }) {}

    fun asProperties(parent: String? = null, required: Boolean = true) =
        validate(
            parent,
            required,
            { isJsonObject && asJsonObject.asMap().all { (_, value) -> value.isJsonPrimitive } }
        ) {}

    fun asRecursiveArray(
        parent: String,
        requiredParent: Boolean = true,
        requiredChildren: Boolean = true,
        validateChildren: JsonValidator.() -> Unit = {}
    ) {
        fun JsonValidator.check(required: Boolean): Unit =
            validate(parent, required, { isJsonArray }) {
                asJsonArray.forEach {
                    it.validateElement(parent, path) {
                        validateChildren()
                        check(requiredChildren)
                    }
                }
            }
        check(requiredParent)
    }

    private inline fun validate(
        parent: String?,
        required: Boolean,
        check: JsonElement.() -> Boolean,
        action: JsonElement.() -> Unit
    ) {
        if (parent == null) {
            if (json.check()) {
                action(json)
                return
            }
            else throwError("JSON validation error: element " +
                    path.joinToString(".") +
                    " have illegal type")
        }
        if (!json.isJsonObject)
            throwError("JSON validation error: element " +
                    path.joinToString(".") +
                    " should be object")
        json.asJsonObject.run {
            if (!has(parent)) {
                if (required)
                    throwError("JSON validation error: element " +
                            (if (path.isEmpty()) "" else path.joinToString(".") + ".") + parent +
                            " is required")
                else return
            }
            get(parent).takeIf(check)?.action()
                ?: throwError("JSON validation error: element " +
                        (if (path.isEmpty()) "" else path.joinToString(".") + ".") + parent +
                        " have illegal type")
        }
        return
    }

    companion object {

        @OptIn(ExperimentalContracts::class)
        inline fun JsonElement.validateElement(
            prev: String? = null,
            path: List<String> = emptyList(),
            validateFunc: JsonValidator.() -> Unit,
        ) {
            contract {
                callsInPlace(validateFunc, InvocationKind.EXACTLY_ONCE)
            }
            JsonValidator(this, path + (prev?.let { listOf(it) } ?: emptyList())).validateFunc()
        }

        @OptIn(ExperimentalContracts::class)
        inline fun <T> JsonReader.parseJsonAndValidate(type: Class<T>, validateFunc: JsonValidator.() -> Unit): T {
            contract {
                callsInPlace(validateFunc, InvocationKind.EXACTLY_ONCE)
            }
            return Streams.parse(this).let {
                it.validateElement { validateFunc() }
                GSON.fromJson(it, type)
            }
        }

        private fun throwError(message: String): Nothing {
            APP_LOGGER.error(message)
            throw IllegalStateException()
        }

    }



}