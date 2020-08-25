package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
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

@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

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
