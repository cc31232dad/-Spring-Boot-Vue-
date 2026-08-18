package com.agromall.seckill.application;

import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import com.agromall.product.domain.Product;
import com.agromall.product.domain.ProductStatus;
import com.agromall.product.infrastructure.ProductMapper;
import com.agromall.seckill.domain.SeckillActivity;
import com.agromall.seckill.domain.SeckillActivityStatus;
import com.agromall.seckill.infrastructure.SeckillActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeckillService {

    private final SeckillActivityMapper activityMapper;
    private final ProductMapper productMapper;
    private final SeckillRedisReservation reservation;

    public SeckillService(SeckillActivityMapper activityMapper, ProductMapper productMapper,
                          SeckillRedisReservation reservation) {
        this.activityMapper = activityMapper;
        this.productMapper = productMapper;
        this.reservation = reservation;
    }

    public SeckillActivity createActivity(Long farmerId, Long productId, BigDecimal price, int stock,
                                          LocalDateTime startAt, LocalDateTime endAt) {
        Product product = productMapper.selectById(productId);
        if (product == null || !product.getFarmerId().equals(farmerId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (stock <= 0 || stock > product.getStock() || !endAt.isAfter(startAt)
                || price.signum() < 0) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_PUBLISHABLE);
        }
        SeckillActivity activity = SeckillActivity.draft(productId, farmerId, price, stock, 1, startAt, endAt);
        activityMapper.insert(activity);
        return activity;
    }

    public void publish(Long activityId) {
        SeckillActivity activity = get(activityId);
        Product product = productMapper.selectById(activity.getProductId());
        if (product == null || !ProductStatus.ON_SALE.name().equals(product.getStatus())
                || activity.getTotalStock() > product.getStock()
                || activityMapper.markPublished(activityId) != 1) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_PUBLISHABLE);
        }
        reservation.warmStock(activityId, activity.getTotalStock());
    }

    public void end(Long activityId) {
        if (activityMapper.markEnded(activityId) != 1) {
            throw new BusinessException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }
    }

    public SeckillActivity get(Long activityId) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.SECKILL_ACTIVITY_NOT_FOUND);
        }
        return activity;
    }

    public List<SeckillActivity> listPublished() {
        return activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, SeckillActivityStatus.PUBLISHED.name())
                .orderByAsc(SeckillActivity::getStartAt));
    }

    public void reserve(Long activityId, Long userId) {
        SeckillActivity activity = get(activityId);
        LocalDateTime now = LocalDateTime.now();
        if (!SeckillActivityStatus.PUBLISHED.name().equals(activity.getStatus()) || now.isBefore(activity.getStartAt())) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (!now.isBefore(activity.getEndAt())) {
            throw new BusinessException(ErrorCode.SECKILL_ENDED);
        }
        reservation.reserve(activityId, userId);
    }
}
