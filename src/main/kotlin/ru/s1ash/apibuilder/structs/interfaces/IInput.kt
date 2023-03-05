package ru.s1ash.apibuilder.structs.interfaces

import ru.s1ash.apibuilder.structs.input.InputChild

interface IInput {
    val fields: Map<String, String>
    val children: List<InputChild>?
}