package ru.s1ash.apibuilder.structs.kit

import ru.s1ash.apibuilder.utils.KIT_FILE
import ru.s1ash.apibuilder.utils.asCheckedPath
import ru.s1ash.apibuilder.utils.measureTime
import ru.s1ash.apibuilder.structs.Block
import ru.s1ash.apibuilder.structs.ReplaceField
import ru.s1ash.apibuilder.structs.interfaces.IContainBlocks
import ru.s1ash.apibuilder.structs.types.ReplaceType
import ru.s1ash.apibuilder.utils.json.JsonValidator.Companion.parseJsonAndValidate
import ru.s1ash.apibuilder.utils.json.useJsonReader
import kotlin.io.path.Path
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile

data class Kit(
    override val id: String,
    val language: String,
    val structure: KitStructure,
    override val blocks: List<Block>,
    val fields: List<ReplaceField>
) : IContainBlocks {

    companion object {
        fun createKit(kitFolderPath: String): Kit =
            measureTime("Kit $kitFolderPath loaded in {} ms") {
                val kitFile = Path(kitFolderPath, KIT_FILE)
                    .asCheckedPath("Unable to access kit file: {}")
                    { isRegularFile() && isReadable() }
                kitFile.useJsonReader {
                    it.parseJsonAndValidate(Kit::class.java) {
                        asPrimitive("id")
                        asPrimitive("language")
                        asObject("structure") {
                            asPrimitive("basement")
                            asPrimitive("target")
                            asPrimitive("env")
                        }
                        asArray("blocks") {
                            asPrimitive("id")
                            asPrimitive("layout")
                            asPrimitive("expandable")
                        }
                        asArray("fields") {
                            asObject {
                                asPrimitive("id")
                                asPrimitive("name", false)
                                asEnum("replace_type", values = ReplaceType.values())
                                asArray("targets", false) { asPrimitive() }
                                asPrimitive("default", false)
                            }
                        }
                    }
                }
            }
    }

}