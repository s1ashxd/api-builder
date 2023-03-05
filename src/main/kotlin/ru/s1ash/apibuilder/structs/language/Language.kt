package ru.s1ash.apibuilder.structs.language

import ru.s1ash.apibuilder.structs.Block
import ru.s1ash.apibuilder.structs.interfaces.IContainBlocks
import ru.s1ash.apibuilder.utils.json.JsonValidator.Companion.parseJsonAndValidate
import ru.s1ash.apibuilder.utils.LANGUAGE_FILE
import ru.s1ash.apibuilder.utils.asCheckedPath
import ru.s1ash.apibuilder.utils.measureTime
import ru.s1ash.apibuilder.utils.json.useJsonReader
import kotlin.io.path.Path
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile

data class Language(
    override val id: String,
    override val blocks: List<Block>
) : IContainBlocks {

    companion object {
        fun createLanguage(languageFolderPath: String) =
            measureTime("Language $languageFolderPath loaded in {} ms") {
                val languageFile = Path(languageFolderPath, LANGUAGE_FILE)
                    .asCheckedPath("Unable to access language file: {}")
                    { isRegularFile() && isReadable() }
                languageFile.useJsonReader {
                    it.parseJsonAndValidate(Language::class.java) {
                        asPrimitive("id")
                        asArray("blocks") {
                            asPrimitive("id")
                            asPrimitive("layout")
                            asPrimitive("expandable")
                        }
                    }
                }
            }
    }

}