package ru.s1ash.apibuilder.structs.extension

import ru.s1ash.apibuilder.structs.Block
import ru.s1ash.apibuilder.structs.ReplaceField
import ru.s1ash.apibuilder.structs.interfaces.IContainBlocks
import ru.s1ash.apibuilder.structs.types.ReplaceType
import ru.s1ash.apibuilder.utils.EXT_FILE
import ru.s1ash.apibuilder.utils.asCheckedPath
import ru.s1ash.apibuilder.utils.json.JsonValidator.Companion.parseJsonAndValidate
import ru.s1ash.apibuilder.utils.measureTime
import ru.s1ash.apibuilder.utils.json.useJsonReader
import kotlin.io.path.Path
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile

data class Extension(
    override val id: String,
    val compatibility: ExtCompatibility,
    val basement: String,
    override val blocks: List<Block>,
    val fields: List<ReplaceField>
) : IContainBlocks {

    companion object{
        fun createExtension(extFolderPath: String) =
            measureTime("Extension $extFolderPath loaded in {} ms") {
                val extFile = Path(extFolderPath, EXT_FILE)
                    .asCheckedPath("Unable to access extension file: {}")
                    { isRegularFile() && isReadable() }
                extFile.useJsonReader {
                    it.parseJsonAndValidate(Extension::class.java) {
                        asPrimitive("id")
                        asObject("compatibility") {
                            asPrimitive("language")
                            asArray("kits") { asPrimitive() }
                            asArray("conflict_kits") { asPrimitive() }
                        }
                        asPrimitive("basement")
                        asArray("blocks") {
                            asPrimitive("id")
                            asPrimitive("layout")
                            asPrimitive("expandable")
                            asArray("fields") { asPrimitive() }
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