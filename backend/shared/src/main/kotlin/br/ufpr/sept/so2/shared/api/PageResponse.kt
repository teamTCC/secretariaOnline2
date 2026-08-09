package br.ufpr.sept.so2.shared.api

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val page: PageMeta,
) {
    data class PageMeta(
        val number: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )

    companion object {
        fun <T> of(page: Page<T>): PageResponse<T> =
            PageResponse(
                content = page.content,
                page =
                    PageMeta(
                        number = page.number,
                        size = page.size,
                        totalElements = page.totalElements,
                        totalPages = page.totalPages,
                    ),
            )

        fun <T, R> of(
            page: Page<T>,
            mapper: (T) -> R,
        ): PageResponse<R> =
            PageResponse(
                content = page.content.map(mapper),
                page =
                    PageMeta(
                        number = page.number,
                        size = page.size,
                        totalElements = page.totalElements,
                        totalPages = page.totalPages,
                    ),
            )
    }
}
