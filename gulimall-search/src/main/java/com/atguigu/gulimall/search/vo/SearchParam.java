package com.atguigu.gulimall.search.vo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParam {
    private String keyword;
    private Long catalog3Id;
    private String sort;
    private Integer hasStock = 1; //0 无，1有
    private String skuPrice;
    private List<Long> brandId;
    private List<String> attrs;
    private Integer pageNum = 1;
}
