/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.util

import org.readium.r2.shared.format.Format
import java.lang.Exception

/**
 * Represents a path on the file system.
 *
 * Used to cache the Format to avoid computing it at different locations.
 */
class File private constructor(
    val file: java.io.File,
    val originalUrl: String? = null,
    private val mediaTypeHint: String? = null,
    private val knownFormat: Format? = null
) {
    /**
     * Creates a File from a path and its known mediaType.
     *
     * @param path Absolute path to the file or directory.
     * @param mediaType If the file's media type is already known, providing it will improve performances.
     */
    constructor(path: String, mediaType: String? = null, originalUrl: String? = null) :
            this(java.io.File(path), originalUrl = originalUrl, mediaTypeHint = mediaType)

    /**
     *  Creates a File from a path and an already resolved format.
     */
    constructor(path: String, format: Format?, originalUrl: String? = null) :
            this(java.io.File(path), originalUrl = originalUrl, knownFormat = format)

    /**
     * Absolute path on the file system.
     */
    val path: String = file.absolutePath

    /**
     * Last path component, or filename.
     */
    val name: String = file.name

    /**
     * Whether the path points to a directory.
     *
     * This can be used to open exploded publication archives.
     */
    val isDirectory: Boolean get() = file.isDirectory

    private lateinit var _format: Try<Format, Exception>

    /**
     * Format, if the path points to a file.
     */
    suspend fun format(): Format? {
        if (!::_format.isInitialized) {
            val format = knownFormat
                ?: Format.ofFile(file, mediaTypeHint)
            _format = format
                ?.let { Try.success(it) }
                ?: Try.failure(Exception("Unable to detect format."))
        }

        return _format.getOrNull()
    }

    override fun equals(other: Any?): Boolean {
        val otherFile = other as? File ?: return false

        return otherFile.file == file
    }

    override fun hashCode(): Int = file.hashCode()
}