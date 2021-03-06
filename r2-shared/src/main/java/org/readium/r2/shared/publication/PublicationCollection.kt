/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.WriteWith
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.shared.JSONable
import org.readium.r2.shared.extensions.JSONParceler
import org.readium.r2.shared.util.logging.WarningLogger
import org.readium.r2.shared.extensions.mapNotNull
import org.readium.r2.shared.extensions.putIfNotEmpty
import org.readium.r2.shared.extensions.toMap
import org.readium.r2.shared.util.logging.JsonWarning
import org.readium.r2.shared.util.logging.log

/**
 * Core Collection Model
 *
 * https://readium.org/webpub-manifest/schema/subcollection.schema.json
 * Can be used as extension point in the Readium Web Publication Manifest.
 *
 * @param role JSON key used to reference this collection in its parent.
 */
@Parcelize
data class PublicationCollection(
    val role: String,
    val metadata: @WriteWith<JSONParceler> Map<String, Any> = emptyMap(),
    val links: List<Link> = emptyList(),
    val otherCollections: List<PublicationCollection> = emptyList()
) : JSONable, Parcelable {

    /**
     * Serializes a [PublicationCollection] to its RWPM JSON representation.
     */
    override fun toJSON() = JSONObject().apply {
        put("metadata", metadata)
        putIfNotEmpty("links", links)
        otherCollections.appendToJSONObject(this)
    }

    companion object {

        fun fromJSON(role: String, json: Any?, normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity): PublicationCollection? =
            fromJSON(role, json, normalizeHref, null)

        /**
         * Parses a [PublicationCollection] from its RWPM JSON representation.
         *
         * If the collection can't be parsed, a warning will be logged with [warnings].
         * The [links]' href and their children's will be normalized recursively using the
         * provided [normalizeHref] closure.
         *
         * @param role JSON key used to reference the collection in its parent.
         */
        internal fun fromJSON(
            role: String,
            json: Any?,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger<JsonWarning>?
        ): PublicationCollection? {
            json ?: return null

            var links: List<Link>
            var metadata: Map<String, Any>? = null
            var otherCollections: List<PublicationCollection>? = null

            when (json) {
                // Parses a sub-collection object.
                is JSONObject -> {
                    links = Link.fromJSONArray(json.remove("links") as? JSONArray, normalizeHref, warnings)
                    metadata = (json.remove("metadata") as? JSONObject)?.toMap()
                    otherCollections = collectionsFromJSON(json, normalizeHref, warnings)
                }

                // Parses an array of links.
                is JSONArray -> {
                    links = Link.fromJSONArray(json, normalizeHref, warnings)
                }

                else -> {
                    warnings?.log(PublicationCollection::class.java, "core collection not valid")
                    return null
                }
            }

            if (links.isEmpty()) {
                warnings?.log(PublicationCollection::class.java, "core collection's [links] must not be empty")
                return null
            }

            return PublicationCollection(
                role = role,
                metadata = metadata ?: emptyMap(),
                links = links,
                otherCollections = otherCollections ?: emptyList()
            )
        }

        fun collectionsFromJSON(json: JSONObject, normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity): List<PublicationCollection> =
            collectionsFromJSON(json, normalizeHref, null)

        /**
         * Parses a list of [PublicationCollection] from its RWPM JSON representation.
         *
         * If the collection can't be parsed, a warning will be logged with [warnings].
         * The [links]' href and their children's will be normalized recursively using the
         * provided [normalizeHref] closure.
         */
        internal fun collectionsFromJSON(
            json: JSONObject,
            normalizeHref: LinkHrefNormalizer = LinkHrefNormalizerIdentity,
            warnings: WarningLogger<JsonWarning>?
        ): List<PublicationCollection> {
            val collections = mutableListOf<PublicationCollection>()
            for (role in json.keys().asSequence().sorted()) {
                val subJSON = json.get(role)

                // Parses a list of links or a single collection object.
                val collection = fromJSON(role, subJSON, normalizeHref, warnings)
                if (collection != null) {
                    collections.add(collection)

                // Parses a list of collection objects.
                } else if (subJSON is JSONArray) {
                    collections.addAll(
                        subJSON.mapNotNull { fromJSON(role, it, normalizeHref, warnings) }
                    )
                }
            }
            return collections
        }

    }

}

/**
 * Serializes a list of [PublicationCollection] into a RWPM JSON representation, where they are
 * indexed by their [role].
 */
internal fun List<PublicationCollection>.toJSONObject(): JSONObject =
    appendToJSONObject(JSONObject())

/**
 * Serializes a list of [PublicationCollection] into a RWPM JSON representation, where they are
 * indexed by their [role], and add them to the given [jsonObject].
 */
internal fun List<PublicationCollection>.appendToJSONObject(jsonObject: JSONObject): JSONObject =
    jsonObject.apply {
        // Groups the sub-collections by their role.
        val collectionsByRole = groupBy(PublicationCollection::role)
        for ((role, collections) in collectionsByRole) {
            if (collections.size == 1) {
                putIfNotEmpty(role, collections.first())
            } else {
                putIfNotEmpty(role, collections)
            }
        }
    }

/**
 * Returns the first [PublicationCollection] with the given [role].
 * This is not recursive.
 */
fun List<PublicationCollection>.firstWithRole(role: String): PublicationCollection? =
    firstOrNull { it.role == role }

/**
 * Returns all the [PublicationCollection] with the given [role].
 * This is not recursive.
 */
fun List<PublicationCollection>.findAllWithRole(role: String): List<PublicationCollection> =
    filter { it.role == role }
