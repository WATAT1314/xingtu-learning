package com.xingtu.learning.coupon.discount;

import com.xingtu.learning.coupon.domain.Coupon;

import java.math.BigDecimal;

/**
 * 优惠券折扣策略
 *
 * 一种券类型对应一个实现，新增券类型只需新增实现并注册，调用方不改代码。
 */
public interface Discount {

    /**
     * 支持的券类型，见 {@link Coupon#TYPE_NO_THRESHOLD} 等常量
     */
    int type();

    /**
     * 计算优惠金额（不会超过订单金额）
     */
    BigDecimal discount(Coupon coupon, BigDecimal amount);
}
