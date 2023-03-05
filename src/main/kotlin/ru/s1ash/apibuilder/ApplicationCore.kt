package ru.s1ash.apibuilder

import ru.s1ash.apibuilder.structs.Block
import ru.s1ash.apibuilder.structs.language.Language
import ru.s1ash.apibuilder.structs.extension.Extension
import ru.s1ash.apibuilder.structs.interfaces.IContainBlocks
import ru.s1ash.apibuilder.structs.kit.Kit
import ru.s1ash.apibuilder.utils.APP_LOGGER
import kotlin.io.path.*

class ApplicationCore(
    private val language: Language,
    private val kit: Kit,
    private val extensions: List<Extension>,
    private val languageFolderPath: String,
    private val kitFolderPath: String,
    private val allExtFolderPath: String
) {

    fun searchBlock(prompt: String): SearchResult {
        val (parentId, blockId) = prompt.split(':')
        val func: (IContainBlocks) -> Boolean = { it.id.equals(parentId, true) }
        val parent = language.takeIf(func)
            ?: kit.takeIf(func)
            ?: extensions.firstOrNull(func)
        if (parent == null) {
            APP_LOGGER.error("Block $prompt not found: owner $parentId not exists")
            throw IllegalStateException()
        }
        val block = parent.blocks.firstOrNull { it.id.equals(blockId, true) }
        if (block == null) {
            APP_LOGGER.error("Block $prompt not found: block $blockId not exists")
            throw IllegalStateException()
        }
        return SearchResult(parent, block)
    }

    fun getBlockLayout(owner: IContainBlocks, block: Block): String {
        val layout = when (owner) {
            is Language -> Path(languageFolderPath, block.layout)
            is Kit -> Path(kitFolderPath, block.layout)
            is Extension -> Path(allExtFolderPath, owner.id, block.layout)
            else -> null
        }
        if (layout == null || layout.notExists() || !layout.isReadable()) {
            APP_LOGGER.error("Unable to load block {} layout: {}", block.id, layout)
            throw IllegalStateException()
        }
        return layout.readText()
    }


}

data class SearchResult(
    val parent: IContainBlocks,
    val block: Block
)
