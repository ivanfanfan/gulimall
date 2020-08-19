package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author ivan
 * @email ivan_sir@gmail.com
 * @date 2020-05-25 00:44:03
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
