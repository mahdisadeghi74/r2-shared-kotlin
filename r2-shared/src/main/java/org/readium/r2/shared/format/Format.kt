/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.format

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.runBlocking
import org.readium.r2.shared.BuildConfig.DEBUG
import org.readium.r2.shared.extensions.queryProjection
import java.io.File


/**
 * Represents a known file format, uniquely identified by a media type.
 *
 * @param name A human readable name identifying the format, which might be presented to the user.
 * @param mediaType The canonical media type that identifies the best (most officially) this format.
 * @param fileExtension The default file extension to use for this format.
 */
data class Format(
    val name: String,
    val mediaType: MediaType,
    val fileExtension: String
) {

    /**
     * Two formats are equal if they have the same media type, regardless of [name] and
     * [fileExtension].
     */
    override fun equals(other: Any?): Boolean =
        mediaType.toString() == (other as? Format)?.mediaType?.toString()

    override fun hashCode(): Int =
        mediaType.hashCode()

    companion object {

        // Default Supported Formats
        //
        // Formats used by Readium. Reading apps are welcome to extend the static constants with
        // additional formats.

        val BMP = Format(
            name = "BMP",
            mediaType = MediaType.BMP,
            fileExtension = "bmp"
        )

        val CBZ = Format(
            name = "Comic Book Archive",
            mediaType = MediaType.CBZ,
            fileExtension = "cbz"
        )

        val DIVINA = Format(
            name = "Digital Visual Narratives",
            mediaType = MediaType.DIVINA,
            fileExtension = "divina"
        )

        val DIVINA_MANIFEST = Format(
            name = "Digital Visual Narratives",
            mediaType = MediaType.DIVINA_MANIFEST,
            fileExtension = "json"
        )

        val EPUB = Format(
            name = "EPUB",
            mediaType = MediaType.EPUB,
            fileExtension = "epub"
        )

        val GIF = Format(
            name = "GIF",
            mediaType = MediaType.GIF,
            fileExtension = "gif"
        )

        val HTML = Format(
            name = "HTML",
            mediaType = MediaType.HTML,
            fileExtension = "html"
        )

        val JPEG = Format(
            name = "JPEG",
            mediaType = MediaType.JPEG,
            fileExtension = "jpg"
        )

        val OPDS1_FEED = Format(
            name = "OPDS",
            mediaType = MediaType.OPDS1,
            fileExtension = "atom"
        )

        val OPDS1_ENTRY = Format(
            name = "OPDS",
            mediaType = MediaType.OPDS1_ENTRY,
            fileExtension = "atom"
        )

        val OPDS2_FEED = Format(
            name = "OPDS",
            mediaType = MediaType.OPDS2,
            fileExtension = "json"
        )

        val OPDS2_PUBLICATION = Format(
            name = "OPDS",
            mediaType = MediaType.OPDS2_PUBLICATION,
            fileExtension = "json"
        )

        val OPDS_AUTHENTICATION = Format(
            name = "OPDS Authentication Document",
            mediaType = MediaType.OPDS_AUTHENTICATION,
            fileExtension = "json"
        )

        val LCP_PROTECTED_AUDIOBOOK = Format(
            name = "LCP Protected Audiobook",
            mediaType = MediaType.LCP_PROTECTED_AUDIOBOOK,
            fileExtension = "lcpa"
        )

        val LCP_PROTECTED_PDF = Format(
            name = "LCP Protected PDF",
            mediaType = MediaType.LCP_PROTECTED_PDF,
            fileExtension = "lcpdf"
        )

        val LCP_LICENSE = Format(
            name = "LCP License",
            mediaType = MediaType.LCP_LICENSE_DOCUMENT,
            fileExtension = "lcpl"
        )

        val LPF = Format(
            name = "Lightweight Packaging Format",
            mediaType = MediaType.LPF,
            fileExtension = "lpf"
        )

        val PDF = Format(
            name = "PDF",
            mediaType = MediaType.PDF,
            fileExtension = "pdf"
        )

        val PNG = Format(
            name = "PNG",
            mediaType = MediaType.PNG,
            fileExtension = "png"
        )

        val READIUM_AUDIOBOOK = Format(
            name = "Readium Audiobook",
            mediaType = MediaType.READIUM_AUDIOBOOK,
            fileExtension = "audiobook"
        )

        val READIUM_AUDIOBOOK_MANIFEST = Format(
            name = "Readium Audiobook",
            mediaType = MediaType.READIUM_AUDIOBOOK_MANIFEST,
            fileExtension = "json"
        )

        val READIUM_WEBPUB = Format(
            name = "Readium Web Publication",
            mediaType = MediaType.READIUM_WEBPUB,
            fileExtension = "webpub"
        )

        val READIUM_WEBPUB_MANIFEST = Format(
            name = "Readium Web Publication",
            mediaType = MediaType.READIUM_WEBPUB_MANIFEST,
            fileExtension = "json"
        )

        val TIFF = Format(
            name = "TIFF",
            mediaType = MediaType.TIFF,
            fileExtension = "tiff"
        )

        val W3C_WPUB_MANIFEST = Format(
            name = "W3C Web Publication",
            mediaType = MediaType.W3C_WPUB_MANIFEST,
            fileExtension = "json"
        )

        val WEBP = Format(
            name = "WebP",
            mediaType = MediaType.WEBP,
            fileExtension = "webp"
        )

        val ZAB = Format(
            name = "Zipped Audio Book",
            mediaType = MediaType.ZAB,
            fileExtension = "zab"
        )


        // Sniffing

        /**
         * The default sniffers provided by Readium 2 to resolve a [Format].
         * You can register additional sniffers globally by modifying this list.
         * The sniffers order is important, because some formats are subsets of other formats.
         */
        val sniffers = FormatSniffers.all.toMutableList()

        /**
         * Resolves a format from a single file extension and media type hint, without checking the actual
         * content.
         */
        fun of(mediaType: String? = null, fileExtension: String? = null, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            if (DEBUG && mediaType?.startsWith("/") == true) {
                throw IllegalArgumentException("The provided media type is incorrect: $mediaType. To pass a file path, you must wrap it in a File().")
            }
            // We use `runBlocking` because checking the media type and file extension is fast.
            return runBlocking { of(content = null, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension), sniffers = sniffers) }
        }

        /**
         * Resolves a format from file extension and media type hints, without checking the actual
         * content.
         */
        fun of(mediaTypes: List<String>, fileExtensions: List<String>, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            // We use `runBlocking` because checking the media type and file extension is fast.
            return runBlocking { of(content = null, mediaTypes = mediaTypes, fileExtensions = fileExtensions, sniffers = sniffers) }
        }

        /**
         * Resolves a format from a local file path.
         */
        suspend fun ofFile(file: File, mediaType: String? = null, fileExtension: String? = null, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return ofFile(file, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension), sniffers = sniffers)
        }

        /**
         * Resolves a format from a local file path.
         */
        suspend fun ofFile(file: File, mediaTypes: List<String>, fileExtensions: List<String>, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return of(content = FormatSnifferFileContent(file), mediaTypes = mediaTypes, fileExtensions = listOf(file.extension) + fileExtensions, sniffers = sniffers)
        }

        /**
         * Resolves a format from a local file path.
         */
        suspend fun ofFile(path: String, mediaType: String? = null, fileExtension: String? = null, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return ofFile(File(path), mediaType = mediaType, fileExtension = fileExtension, sniffers = sniffers)
        }

        /**
         * Resolves a format from a local file path.
         */
        suspend fun ofFile(path: String, mediaTypes: List<String>, fileExtensions: List<String>, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return ofFile(File(path), mediaTypes = mediaTypes, fileExtensions = fileExtensions, sniffers = sniffers)
        }

        /**
         * Resolves a format from bytes, e.g. from an HTTP response.
         */
        suspend fun ofBytes(bytes: () -> ByteArray, mediaType: String? = null, fileExtension: String? = null, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return ofBytes(bytes, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension), sniffers = sniffers)
        }

        /**
         * Resolves a format from bytes, e.g. from an HTTP response.
         */
        suspend fun ofBytes(bytes: () -> ByteArray, mediaTypes: List<String>, fileExtensions: List<String>, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return of(content = FormatSnifferBytesContent(bytes), mediaTypes = mediaTypes, fileExtensions = fileExtensions, sniffers = sniffers)
        }

        /**
         * Resolves a format from a content URI and a [ContentResolver].
         * Accepts the following URI schemes: content, android.resource, file.
         */
        suspend fun ofUri(uri: Uri, contentResolver: ContentResolver, mediaType: String? = null, fileExtension: String? = null, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return ofUri(uri, contentResolver, mediaTypes = listOfNotNull(mediaType), fileExtensions = listOfNotNull(fileExtension), sniffers = sniffers)
        }

        /**
         * Resolves a format from a content URI and a [ContentResolver].
         * Accepts the following URI schemes: content, android.resource, file.
         */
        suspend fun ofUri(uri: Uri, contentResolver: ContentResolver, mediaTypes: List<String>, fileExtensions: List<String>, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            val allMediaTypes = mediaTypes.toMutableList()
            val allFileExtensions = fileExtensions.toMutableList()

            MimeTypeMap.getFileExtensionFromUrl(uri.toString()).ifEmpty { null }?.let {
                allFileExtensions.add(0, it)
            }

            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                contentResolver.getType(uri)
                    ?.takeUnless { MediaType.BINARY.matches(it) }
                    ?.let { allMediaTypes.add(0, it) }

                contentResolver.queryProjection(uri, MediaStore.MediaColumns.DISPLAY_NAME)?.let { filename ->
                    allFileExtensions.add(0, File(filename).extension)
                }
            }

            val content = FormatSnifferUriContent(uri = uri, contentResolver = contentResolver)
            return of(content = content, mediaTypes = allMediaTypes, fileExtensions = allFileExtensions, sniffers = sniffers)
        }

        /**
         * Resolves a format from a sniffer context.
         *
         * Sniffing a format is done in two rounds, because we want to give an opportunity to all
         * sniffers to return a [Format] quickly before inspecting the content itself:
         *  - Light Sniffing checks only the provided file extension or media type hints.
         *  - Heavy Sniffing reads the bytes to perform more advanced sniffing.
         */
        private suspend fun of(content: FormatSnifferContent?, mediaTypes: List<String>, fileExtensions: List<String>, sniffers: List<FormatSniffer>): Format? {
            // Light sniffing
            var context = FormatSnifferContext(mediaTypes = mediaTypes, fileExtensions = fileExtensions)
            for (sniffer in sniffers) {
                val format = sniffer(context)
                if (format != null) {
                    return format
                }
            }

            // Heavy sniffing
            if (content != null) {
                context = FormatSnifferContext(content = content, mediaTypes = mediaTypes, fileExtensions = fileExtensions)
                for (sniffer in sniffers) {
                    val format = sniffer(context)
                    if (format != null) {
                        return format
                    }
                }
            }

            // Falls back on the system-wide registered media types using [MimeTypeMap].
            // Note: This is done after the heavy sniffing of the provided [sniffers], because
            // otherwise it will detect JSON, XML or ZIP formats before we have a chance of sniffing
            // their content (for example, for RWPM).
            return FormatSniffers.system(context)
        }

        @Deprecated("Renamed to [ofFile()]", ReplaceWith("Format.ofFile(file, mediaType, fileExtension, sniffers)"))
        fun of(file: File, mediaType: String? = null, fileExtension: String? = null, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return runBlocking { ofFile(file, listOfNotNull(mediaType), listOfNotNull(fileExtension), sniffers) }
        }

        @Deprecated("Renamed to [ofFile()]", ReplaceWith("Format.ofFile(file, mediaTypes, fileExtensions, sniffers)"))
        fun of(file: File, mediaTypes: List<String>, fileExtensions: List<String>, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return runBlocking { ofFile(file, mediaTypes = mediaTypes, fileExtensions = listOf(file.extension) + fileExtensions, sniffers = sniffers) }
        }

        @Deprecated("Renamed to [ofBytes()]", ReplaceWith("Format.ofBytes(bytes, mediaType, fileExtension, sniffers)"))
        fun of(bytes: () -> ByteArray, mediaType: String? = null, fileExtension: String? = null, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return runBlocking { ofBytes(bytes, listOfNotNull(mediaType), listOfNotNull(fileExtension), sniffers) }
        }

        @Deprecated("Renamed to [ofBytes()]", ReplaceWith("Format.ofBytes(bytes, mediaTypes, fileExtensions, sniffers)"))
        fun of(bytes: () -> ByteArray, mediaTypes: List<String>, fileExtensions: List<String>, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return runBlocking { ofBytes(bytes, mediaTypes, fileExtensions, sniffers) }
        }

        @Deprecated("Renamed to [ofUri()]", ReplaceWith("Format.ofUri(uri, contentResolver, mediaType, fileExtension, sniffers)"))
        fun of(uri: Uri, contentResolver: ContentResolver, mediaType: String? = null, fileExtension: String? = null, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return runBlocking { ofUri(uri, contentResolver, listOfNotNull(mediaType), listOfNotNull(fileExtension), sniffers) }
        }

        @Deprecated("Renamed to [ofUri()]", ReplaceWith("Format.ofUri(uri, contentResolver, mediaTypes, fileExtensions, sniffers)"))
        fun of(uri: Uri, contentResolver: ContentResolver, mediaTypes: List<String>, fileExtensions: List<String>, sniffers: List<FormatSniffer> = Format.sniffers): Format? {
            return runBlocking { ofUri(uri, contentResolver, mediaTypes, fileExtensions, sniffers) }
        }

        @Deprecated("Use [READIUM_AUDIOBOOK] instead", ReplaceWith("READIUM_AUDIOBOOK"))
        val AUDIOBOOK: Format get() = READIUM_AUDIOBOOK
        @Deprecated("Use [READIUM_AUDIOBOOK_MANIFEST] instead", ReplaceWith("READIUM_AUDIOBOOK_MANIFEST"))
        val AUDIOBOOK_MANIFEST: Format get() = READIUM_AUDIOBOOK_MANIFEST
        @Deprecated("Use [READIUM_WEBPUB] instead", ReplaceWith("READIUM_WEBPUB"))
        val WEBPUB: Format get() = READIUM_WEBPUB
        @Deprecated("Use [READIUM_WEBPUB_MANIFEST] instead", ReplaceWith("READIUM_WEBPUB_MANIFEST"))
        val WEBPUB_MANIFEST: Format get() = READIUM_WEBPUB_MANIFEST

    }

}