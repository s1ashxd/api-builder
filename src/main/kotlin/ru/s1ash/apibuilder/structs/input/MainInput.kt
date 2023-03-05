package ru.s1ash.apibuilder.structs.input

import ru.s1ash.apibuilder.structs.interfaces.IInput
import ru.s1ash.apibuilder.utils.asCheckedPath
import ru.s1ash.apibuilder.utils.json.JsonValidator.Companion.parseJsonAndValidate
import ru.s1ash.apibuilder.utils.measureTime
import ru.s1ash.apibuilder.utils.json.useJsonReader
import kotlin.io.path.Path
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile

data class MainInput(
    val preferences: Map<String, String>,
    override val fields: Map<String, String>,
    override val children: List<InputChild>
) : IInput {

    companion object {
        fun createInput(inputFilePath: String) =
            measureTime("Input loaded in {} ms") {
                val inputFile = Path(inputFilePath)
                    .asCheckedPath("Unable to access input file: {}")
                    { isRegularFile() && isReadable() }
                inputFile.useJsonReader {
                    it.parseJsonAndValidate(MainInput::class.java) {
                        asProperties("preferences")
                        asProperties("fields")
                        asRecursiveArray("children", requiredParent = true, requiredChildren = false) {
                            asPrimitive("id")
                            asProperties("fields")
                        }
                    }
                }
            }
    }

}