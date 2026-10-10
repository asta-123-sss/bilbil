package com.bilbil.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页返回对象
 *
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private long total;
    /** 当前页码（从 1 开始） */
    private long page;
    /** 每页大小 */
    private long size;
    /** 当前页数据 */
    private List<T> list;

    public PageResult() {
    }

    public PageResult(long total, long page, long size, List<T> list) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.list = list;
    }

    /** 构建分页对象 */
    public static <T> PageResult<T> of(long total, long page, long size, List<T> list) {
        return new PageResult<>(total, page, size, list);
    }

    /** 计算总页数 */
    public long getPages() {
        return size <= 0 ? 0 : (total + size - 1) / size;
    }
}
