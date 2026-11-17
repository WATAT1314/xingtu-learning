package com.xingtu.learning.coupon;

import com.xingtu.learning.common.Result;
import com.xingtu.learning.coupon.domain.Coupon;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 优惠券接口
 */
@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /** 领取优惠券（Redis + Lua 防超发） */
    @PostMapping("/{couponId}/receive")
    public Result<Map<String, Object>> receive(@PathVariable Long couponId, @RequestParam Long userId) {
        return Result.ok(couponService.receive(userId, couponId));
    }

    /** 我的可用券 */
    @GetMapping("/mine")
    public Result<List<Coupon>> mine(@RequestParam Long userId) {
        return Result.ok(couponService.available(userId));
    }

    /** 计算最优优惠组合 */
    @PostMapping("/best")
    public Result<Map<String, Object>> best(@RequestBody BestForm form) {
        return Result.ok(couponService.best(form.userId(), form.orderAmount()));
    }

    public record BestForm(Long userId, BigDecimal orderAmount) {
    }
}
