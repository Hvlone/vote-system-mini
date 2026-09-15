package com.vote.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用分页响应体
 * @param <T> 列表元素类型
 */
@Data
public class PageVO<T> {

    /** 当前页数据 */
    private List<T> list;

    /** 满足条件的总条数 */
    private long total;

    /** 当前页码，从 1 开始 */
    private int page;

    /** 每页条数 */
    private int size;

    /** 是否还有下一页，前端据此决定能否继续触底加载 */
    private boolean hasMore;

    public static <T> PageVO<T> of(List<T> list, long total, int page, int size) {
        PageVO<T> vo = new PageVO<>();
        vo.list = list;
        vo.total = total;
        vo.page = page;
        vo.size = size;
        vo.hasMore = (long) page * size < total;
        return vo;
    }
}
