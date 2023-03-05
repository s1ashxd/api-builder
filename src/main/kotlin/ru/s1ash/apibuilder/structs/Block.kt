package ru.s1ash.apibuilder.structs

import ru.s1ash.apibuilder.structs.interfaces.IHasIdentifier

data class Block(
    override val id: String,
    val layout: String,
    val expandable: Boolean
) : IHasIdentifier