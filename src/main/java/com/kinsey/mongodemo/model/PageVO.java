package com.kinsey.mongodemo.model;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageVO<T> implements Serializable {

    private int pageNum;

    private int pageSize;

    private long total;

    private int pages;

    private List<T> data;

    public static <T> PageVO<T> build(Page<T> page) {
        return new PageVO<>(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.getContent());
    }

    public static <T, E> PageVO<T> build(Page<E> poPage, List<T> voList) {
        return new PageVO<>(poPage.getNumber(), poPage.getSize(), poPage.getTotalElements(), poPage.getTotalPages(), voList);
    }
}