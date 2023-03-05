package ru.s1ash.apibuilder.structs.extension

import com.google.gson.annotations.SerializedName
import ru.s1ash.apibuilder.structs.language.Language
import ru.s1ash.apibuilder.structs.kit.Kit
import ru.s1ash.apibuilder.utils.ALL

data class ExtCompatibility(
    val language: String,
    val kits: List<String>,
    @SerializedName("conflict_kits")
    val conflictKits: List<String>
) {

    fun check(lang: Language, kit: Kit): Boolean {
        if (!language.equals(lang.id, true)) return false
        if (conflictKits.any { it.equals(kit.id, true) }) return false
        kits.forEach {
            if (it == ALL) return true
            if (it.equals(kit.id, true)) return true
        }
        return false
    }

}
