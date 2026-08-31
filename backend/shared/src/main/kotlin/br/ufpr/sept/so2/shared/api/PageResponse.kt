package br.ufpr.sept.so2.shared.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.domain.Page
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PageResponse<T>(
    val content: List<T>,
    val page: PageMeta,
    @JsonProperty("_links")
    val links: PageLinks? = null,
) {
    data class PageMeta(
        val number: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )

    data class PageLinks(
        val self: String,
        val first: String,
        val last: String,
        val next: String? = null,
        val prev: String? = null,
    )

    companion object {
        fun <T> of(page: Page<T>): PageResponse<T> =
            PageResponse(
                content = page.content,
                page = page.toMeta(),
            )

        fun <T, R> of(
            page: Page<T>,
            mapper: (T) -> R,
        ): PageResponse<R> =
            PageResponse(
                content = page.content.map(mapper),
                page = page.toMeta(),
            )

        /** Builds a PageResponse with HAL-style navigation links derived from the current request URI. */
        fun <T, R> ofWithLinks(
            page: Page<T>,
            mapper: (T) -> R,
        ): PageResponse<R> {
            val base =
                try {
                    ServletUriComponentsBuilder.fromCurrentRequest()
                        .replaceQueryParam("page")
                        .replaceQueryParam("size")
                        .build()
                        .toUriString()
                        .trimEnd('?', '&')
                } catch (_: IllegalStateException) {
                    null
                }

            fun pageUrl(p: Int): String? {
                if (base == null) return null
                val sep = if (base.contains('?')) '&' else '?'
                return "$base${sep}page=$p&size=${page.size}"
            }

            val links =
                if (base != null) {
                    PageLinks(
                        self = pageUrl(page.number)!!,
                        first = pageUrl(0)!!,
                        last = pageUrl((page.totalPages - 1).coerceAtLeast(0))!!,
                        next = if (page.hasNext()) pageUrl(page.number + 1) else null,
                        prev = if (page.hasPrevious()) pageUrl(page.number - 1) else null,
                    )
                } else {
                    null
                }

            return PageResponse(
                content = page.content.map(mapper),
                page = page.toMeta(),
                links = links,
            )
        }

        private fun <T> Page<T>.toMeta() =
            PageMeta(
                number = number,
                size = size,
                totalElements = totalElements,
                totalPages = totalPages,
            )
    }
}
