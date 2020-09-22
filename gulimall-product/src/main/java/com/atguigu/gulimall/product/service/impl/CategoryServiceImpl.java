package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sun.org.apache.xml.internal.resolver.CatalogEntry;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    RedissonClient redissonClient;
    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查出所有的分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2.组装成父子结构
        //2.1找到所有的1级分类
        //使用filter过滤器过滤全部分类，返回stream里面变成了父分类id为0
        List<CategoryEntity> levelOneMenus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu)->{//map映射子分类为树结构
            menu.setChildren(getChildren(menu,entities));
            return menu;
        }).sorted((menu1,menu2)->{//排序
            return (menu1.getSort()==null?0:menu1.getSort())- (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return levelOneMenus;//返回一级分类 分类实体中建立了children字段 所以一级分类包含了 子分类。。。
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 检查菜单是否被引用
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCateLogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parenPath = findParenPath(catelogId, paths);
        Collections.reverse(parenPath);
        return parenPath.toArray(new Long[parenPath.size()]);
    }

    @Transactional
    @Override
    public void upCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> entities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return entities;
    }

    /**
     * 使用redis 查询
     * redis 缓存穿透，雪崩，击穿
     * //TODO 高并发下产生堆外内存异常，应该是元空间溢出，OutOfDirectMemoryError
     * springboot 2.0使用Lettuce操作redis，他使用netty进行网络间通信。
     * Lettuce的bug导致netty堆外内存溢出
     * @return
     */
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson(){
        String catalogJson = redisTemplate.opsForValue().get("catelogJson");
        //redis中没数据的时候 从数据库中查找并放入redis
        if(StringUtils.isEmpty(catalogJson)){
            System.out.println("MYSQL————————————————————————");
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromDBWithRedissonLock();
            String s = JSON.toJSONString(catalogJsonFromDB);
            return catalogJsonFromDB;
        }
        //不为空
        System.out.println("Redis------------------");
        Map<String,List<Catelog2Vo>> result = JSON.parseObject(catalogJson,new TypeReference<Map<String,List<Catelog2Vo>>>(){});
        return result;
    }

    /**
     * Redisson分布式锁
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedissonLock(){
        RLock lock = redissonClient.getLock("CatalogJson-lock");
        lock.lock();
        System.out.println("获取分布式锁成功");
        Map<String,List<Catelog2Vo>> dataFromDb;
        try{
            dataFromDb = getCatalogJsonFromDB();
        }finally {
            lock.unlock();
        }
        return dataFromDb;
    }
    /**
     * 从数据库查询三级分类数据
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDB() {

        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //查找一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList,0L);
        //查找二级
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            List<CategoryEntity> categoryEntities = getParent_cid(selectList,v.getCatId());
            //找二级分类
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null && !categoryEntities.isEmpty()) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //找三级分类
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList,l2.getCatId());
                    if(level3Catelog!=null){
                        List<Catelog2Vo.Catalog3Vo> catalog3Vos = level3Catelog.stream().map(l3 -> {
                            Catelog2Vo.Catalog3Vo catalog3Vo = new Catelog2Vo.Catalog3Vo(l2.getCatId().toString(),l3.getCatId().toString(),l3.getName());

                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catalog3Vos);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));

        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catelogJson",s,1, TimeUnit.DAYS);
        return parent_cid;
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> {
            return item.getParentCid() == parent_cid;
        }).collect(Collectors.toList());
//        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
        return collect;
    }

    private List<Long> findParenPath(Long catelogId,List<Long> paths){
        //1.收集当前结点ID
        paths.add(catelogId);
        CategoryEntity category = this.getById(catelogId);
        if(category.getParentCid()!= 0){
            findParenPath(category.getParentCid(),paths);
        }
        return paths;
    }
        //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> entities) {
        List<CategoryEntity> children = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            categoryEntity.setChildren(getChildren(categoryEntity, entities));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

}
