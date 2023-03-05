package ru.s1ash.apibuilder.utils.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import ru.s1ash.apibuilder.structs.ReplaceField
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.reader

@JvmField
val GSON: Gson = GsonBuilder()
    .registerTypeAdapter(ReplaceField::class.java, ReplaceField.Deserializer)
    .create()

@OptIn(ExperimentalContracts::class)
inline fun <T> Path.useJsonReader(block: (JsonReader) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return reader().use {
        GSON.newJsonReader(it).use(block)
    }
}
