package com.xingtu.learning.coupon.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板
 */
public record Coupon(Long id,
                     String name,
                     int type,
                     BigDecimal thresholdAmount,
                     BigDecimal discountAmount,
                     BigDecimal discountRate,
                     BigDecimal perAmount,
                     int totalNum,
                     int remainNum,
                     LocalDateTime beginTime,
                     LocalDateTime endTime) {

    public static final int TYPE_NO_THRESHOLD = 1;
    public static final int TYPE_PRICE_DISCOUNT = 2;
    public static final int TYPE_PER_PRICE_DISCOUNT = 3;
    public static final int TYPE_RATE_DISCOUNT = 4;
}
