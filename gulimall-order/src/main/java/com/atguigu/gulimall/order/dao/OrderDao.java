package com.atguigu.gulimall.order.dao;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author ivan
 * @email ivan_sir@gmail.com
 * @date 2020-05-25 01:13:04
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
