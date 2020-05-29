/*
 * Module: r2-shared-kotlin
 * Developers: Quentin Gliosca
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.fetcher

import org.readium.r2.shared.publication.Link

typealias HrefParameters = Map<String, String>

/** Provides access to a [Resource] from a [Link]. */
interface Fetcher {

    /**
     * Known resources available in the medium, such as file paths on the file system
     * or entries in a ZIP archive. This list is not exhaustive, and additional
     * unknown resources might be reachable.
     *
     * If the medium has an inherent resource order, it should be followed.
     * Otherwise, HREFs are sorted alphabetically.
     */
    val links: List<Link>

    /**
     * Returns the [Resource] at the given [link]'s HREF.
     *
     * A [Resource] is always returned, since for some cases we can't know if it exists before
     * actually fetching it, such as HTTP. Therefore, errors are handled at the Resource level.
     *
     * You can provide HREF [parameters] that the source will understand, such as:
     *  - when [link] is templated,
     *  - to append additional query parameters to an HTTP request.
     *
     * The [parameters] are expected to be percent-decoded.
     */
    fun get(link: Link, parameters: HrefParameters = emptyMap()): Resource

    /** Closes any opened file handles, removes temporary files, etc. */
    fun close()

}

/** A [Fetcher] providing no resources at all. */
class EmptyFetcher : Fetcher {

    override val links: List<Link> = emptyList()

    override fun get(link: Link, parameters: HrefParameters): Resource = FailureResource(link, Resource.Error.NotFound)

    override fun close() {}

}
