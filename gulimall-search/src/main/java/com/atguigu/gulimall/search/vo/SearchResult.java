package com.atguigu.gulimall.search.vo;

import com.atguigu.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

@Data
public class SearchResult {
    //查询到的所有商品的信息
    private List<SkuEsModel> products;
    /**
     * 分页信息
     */
    private Integer pageNum;
    private Long total;
    private Integer totalPage;
    private List<BrandVo> brands;
    private List<CatalogVo> catalogs;
    private List<AttrVo> attrs;
    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class AttrVo {
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }


    @Data
    public static class CatalogVo {
        private Long catalogId;
        private String catalogName;
    }

}
