package ru.s1ash.apibuilder.utils

import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.*

@OptIn(ExperimentalContracts::class)
fun Path.asCheckedPath(placeholder: String, block: Path.() -> Boolean): Path {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (block()) return this
    APP_LOGGER.error(placeholder, absolutePathString())
    throw IllegalAccessException()
}

fun Path.asWritableFile(placeholder: String, clean: Boolean = false): Path {
    if (clean) deleteIfExists()
    if (notExists()) {
        if (parent.notExists())
            parent.createDirectories()
        createFile()
    }
    if (!isWritable()) {
        APP_LOGGER.error(placeholder, absolutePathString())
        throw IllegalAccessException()
    }
    return this
}

fun Path.asDirectories(): Path {
    if (notExists())
        createDirectories()
    return this
}

@OptIn(ExperimentalPathApi::class)
fun Path.copyRecursively(target: Path) {
    if (notExists()) {
        APP_LOGGER.error("Source file doesn't exists: {}", this.absolutePathString())
        throw IllegalStateException()
    }
    walk(PathWalkOption.INCLUDE_DIRECTORIES).forEach {
        if (it.notExists()) {
            APP_LOGGER.error("Source file doesn't exists: {}", this.absolutePathString())
            throw IllegalStateException()
        }
        val rel = this.relativize(it).toString()
        if (rel.isBlank()) return@forEach
        val dst = Path(target.absolutePathString(), rel)
        if (dst.exists() && !(it.isDirectory() && dst.isDirectory())) {
            val stillExists = if (dst.isDirectory()) dst.deleteRecursively()
            else {
                dst.deleteExisting()
                dst.exists()
            }
            if (stillExists) {
                APP_LOGGER.error("The destination files already exists: {}", dst)
                throw IllegalStateException()
            }
        }
        if (it.isDirectory())
            dst.createDirectories()
        else {
            if (it.copyTo(dst, true).fileSize() != dst.fileSize()) {
                APP_LOGGER.error("Source file wasn't copied completely, length of destination file differs")
                throw IllegalStateException()
            }
        }
    }
}

@OptIn(ExperimentalPathApi::class)
fun Path.deleteRecursively() =
    walk(PathWalkOption.INCLUDE_DIRECTORIES).fold(true) { res, path -> path.deleteIfExists() && path.notExists() && res }