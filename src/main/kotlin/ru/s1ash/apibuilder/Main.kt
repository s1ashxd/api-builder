package ru.s1ash.apibuilder

import ru.s1ash.apibuilder.structs.kit.Kit
import ru.s1ash.apibuilder.structs.language.Language
import ru.s1ash.apibuilder.structs.extension.Extension
import ru.s1ash.apibuilder.structs.input.MainInput
import ru.s1ash.apibuilder.structs.interfaces.IInput
import ru.s1ash.apibuilder.structs.types.ReplaceType
import ru.s1ash.apibuilder.utils.*
import java.util.regex.Pattern
import kotlin.io.path.*
import kotlin.text.StringBuilder

fun main(args: Array<String>) {
    val startTime = System.currentTimeMillis()
    val parsedArgs = args.toArgumentsMap()
    log {
        info("API Builder v $VERSION started")
        info("Arguments: $parsedArgs")
    }

    val kitFolder = Path(
        parsedArgs.getOrDefault(KITS_DIR_ARGUMENT_NAME, KITS_DEFAULT_DIR),
        parsedArgs.getOrDefault(KIT_ARGUMENT_NAME, DEFAULT_KIT)
    ).asCheckedPath("Unable to access kit folder: {}") { isDirectory() && isReadable() }
    val kitFolderPath = kitFolder.absolutePathString()
    APP_LOGGER.info("Selected kit: $kitFolderPath")

    val kit = Kit.createKit(kitFolderPath)

    val languageFolder = Path(
        parsedArgs.getOrDefault(LANGUAGES_DIR_ARGUMENT_NAME, LANGUAGES_DEFAULT_DIR),
        kit.language
    ).asCheckedPath("Unable to access language folder: {}") { isDirectory() && isReadable() }
    val languageFolderPath = languageFolder.absolutePathString()
    APP_LOGGER.info("Selected language: $languageFolderPath")

    val language = Language.createLanguage(languageFolderPath)

    val allExtFolder = Path(parsedArgs.getOrDefault(EXT_DIR_ARGUMENT_NAME, DEFAULT_EXT_DIR))
        .asCheckedPath("Unable to access all extensions folder: {}")
        { isDirectory() && isReadable() }
    val allExtFolderPath = allExtFolder.absolutePathString()

    val extensions = parsedArgs.getOrDefault(EXT_ARGUMENT_NAME, DEFAULT_EXTENSIONS)
        .split(';')
        .filter { it.isNotBlank() }
        .mapNotNull {
            val extFolder = Path(allExtFolderPath, it)
                .asCheckedPath("Unable to access extension folder: {}")
                { isDirectory() && isReadable() }
            val extFolderPath = extFolder.absolutePathString()
            APP_LOGGER.info("Try to use extension: {}", extFolderPath)
            val extension = Extension.createExtension(extFolderPath)
            if (extension.compatibility.check(language, kit)) extension
            else {
                APP_LOGGER.warn("Extension ${extension.id} is not compatible for kit ${kit.id} with language ${language.id}")
                null
            }
        }

    val core = ApplicationCore(language, kit, extensions, languageFolderPath, kitFolderPath, allExtFolderPath)

    APP_LOGGER.info("All environment loaded successfully")

    val input = MainInput.createInput(parsedArgs.getOrDefault(INPUT_ARGUMENT_NAME, DEFAULT_INPUT_FILE))

    val outputFolder = Path(parsedArgs.getOrDefault(OUTPUT_ARGUMENT_NAME, DEFAULT_OUTPUT_FOLDER)).asDirectories()
    val outputFolderPath = outputFolder.absolutePathString()

    measureTime("Kit and extensions basement copied in {} ms") {
        Path(kitFolderPath, kit.structure.basement)
            .takeIf { it.exists() && it.isReadable() }
            ?.copyRecursively(outputFolder)
        extensions.forEach { ext ->
            Path(allExtFolderPath, ext.id, ext.basement)
                .takeIf { it.exists() && it.isReadable() }
                ?.copyRecursively(outputFolder)
        }
    }

    val contentPattern = Pattern.compile("%content%", Pattern.CASE_INSENSITIVE)
    val fieldPattern = Pattern.compile("%(\\w*?)%")
    fun recursiveCreateContent(builder: StringBuilder, currentInput: IInput, tabulations: Int) {
        currentInput.children?.forEach {
            val (owner, block) = core.searchBlock(it.id)
            val layout = StringBuilder(core.getBlockLayout(owner, block))
            if (block.expandable) {
                val childBuilder = StringBuilder()
                recursiveCreateContent(childBuilder, it, tabulations + 1)
                val matcher = contentPattern.matcher(layout)
                while (matcher.find())
                    layout.replace(matcher.start(), matcher.end(), childBuilder.toString().trimEnd())
            }
            var matcher = fieldPattern.matcher(layout)
            while (matcher.find()) {
                val key = matcher.group(1)
                val value = it.fields[key] ?: continue
                if (owner is Kit) {
                    if (owner.fields.none { field ->
                            field.id.equals(key, true)
                                    && field.targets.contains(owner.structure.target) }) continue
                }
                layout.replace(matcher.start(), matcher.end(), value)
                matcher = fieldPattern.matcher(layout)
            }
            builder.append(
                " ".repeat(input.preferences["tab_size"]?.toIntOrNull() ?: 4)
                    .repeat(tabulations)
            ).append(layout).append(input.preferences.getOrDefault("line_separator", "\r\n"))
        }
    }

    measureTime("Target content generated and applied in {} ms") {
        val builder = StringBuilder()
        recursiveCreateContent(builder, input, 0)
        val kitLayoutFile = Path(outputFolderPath, kit.structure.target)
            .asCheckedPath("Unable to access kit layout: {}")
            { isRegularFile() && isReadable() && isWritable() }
        val kitLayout = kitLayoutFile.readText().replace("%content%", builder.toString(), true)
        kitLayoutFile.writeText(kitLayout)
    }

    measureTime("All fields replaced in {} ms") {
        kit.fields.forEach { field ->
            when (field.replaceType) {
                ReplaceType.PLACEHOLDER -> {
                    field.targets.forEach {
                        val fieldFile = Path(outputFolderPath, it)
                            .asCheckedPath("Unable to access file with field ${field.id}: {}")
                            { isRegularFile() && isReadable() && isWritable() }
                        val fieldValue = input.fields[field.id] ?: field.defaults
                        if (fieldValue == null) {
                            APP_LOGGER.error("Input must contain a required property: {}", field.id)
                            throw IllegalStateException()
                        }
                        val fileContent = fieldFile.readText()
                            .replace("%${field.id}%", fieldValue)
                        fieldFile.writeText(fileContent)
                    }
                }
                ReplaceType.ENV -> {
                    val envFile = Path(outputFolderPath, kit.structure.env)
                        .asWritableFile("Unable to write data in env file: {}")
                    val fieldValue = input.fields[field.id] ?: field.defaults
                    if (fieldValue == null) {
                        APP_LOGGER.error("Input must contain a required env property: {}", field.id)
                        throw IllegalStateException()
                    }
                    envFile.appendText("${field.name}=$fieldValue")
                }
            }
        }
    }

    log {
        info("Code generated successfully!")
        info("Execution time: {} ms", System.currentTimeMillis() - startTime)
        info("Output folder: {}", outputFolderPath)
    }
}