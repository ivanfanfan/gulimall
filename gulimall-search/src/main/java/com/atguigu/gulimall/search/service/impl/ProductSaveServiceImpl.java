package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    /**
     * 第一步: 注入RestHighLevelClient restHighLevelClient 用来操作es
     * 第二步：给es建立索引product ,建立好映射关系。
     * 第三步：给es中保存数据
     * @param skuEsModels
     */
    @Autowired
    RestHighLevelClient restHighLevelClient;
    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        //3保存数据public final BulkResponse bulk(BulkRequest bulkRequest, RequestOptions options)

        BulkRequest bulkRequest = new BulkRequest();
        for(SkuEsModel model : skuEsModels){
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(String.valueOf(model.getSkuId()));
            String s = JSON.toJSONString(model);
            indexRequest.source(s, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        //TODO if bulk error
        boolean b = bulk.hasFailures();
        List<String> collect = Arrays.stream(bulk.getItems()).map(bulkItemResponse -> {
            return bulkItemResponse.getId();
        }).collect(Collectors.toList());
        log.info("商品上架完成:{}",collect);
        return b;
    }
}
