package com.testpilot.platform.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 稳定的分页响应结构。
 *
 * <p>直接序列化 Spring Data 的 {@code PageImpl} 会得到不保证稳定的 JSON 结构（Spring 会打
 * 警告），统一通过该 DTO 输出，字段固定为 content/page/size/totalElements/totalPages。
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
