package ru.s1ash.apibuilder.structs.input

import ru.s1ash.apibuilder.structs.interfaces.IInput

data class InputChild(
    val id: String,
    override val fields: Map<String, String>,
    override val children: List<InputChild>?
) : IInput