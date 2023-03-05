package ru.s1ash.apibuilder.structs.interfaces

import ru.s1ash.apibuilder.structs.Block

interface IContainBlocks : IHasIdentifier {
    val blocks: List<Block>
}