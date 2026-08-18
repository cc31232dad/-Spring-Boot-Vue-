package com.agromall.seckill.application;

import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import com.agromall.product.domain.Product;
import com.agromall.product.domain.ProductStatus;
import com.agromall.product.infrastructure.ProductMapper;
import com.agromall.order.api.OrderItemView;
import com.agromall.order.api.OrderView;
import com.agromall.order.domain.Order;
import com.agromall.order.domain.OrderItem;
import com.agromall.order.infrastructure.OrderItemMapper;
import com.agromall.order.infrastructure.OrderMapper;
import com.agromall.seckill.api.SeckillActivityView;
import com.agromall.seckill.api.SeckillRequest;
import com.agromall.seckill.domain.SeckillActivity;
import com.agromall.seckill.domain.SeckillActivityStatus;
import com.agromall.seckill.domain.SeckillOrder;
import com.agromall.seckill.infrastructure.SeckillActivityMapper;
import com.agromall.seckill.infrastructure.SeckillOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SeckillService {

    private final SeckillActivityMapper activityMapper;
    private final ProductMapper productMapper;
    private final SeckillRedisReservation reservation;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SeckillOrderMapper seckillOrderMapper;

    public SeckillService(SeckillActivityMapper activityMapper, ProductMapper productMapper,
                          SeckillRedisReservation reservation, OrderMapper orderMapper,
                          OrderItemMapper orderItemMapper, SeckillOrderMapper seckillOrderMapper) {
        this.activityMapper = activityMapper;
        this.productMapper = productMapper;
        this.reservation = reservation;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.seckillOrderMapper = seckillOrderMapper;
    }

    public SeckillActivity createActivity(Long farmerId, Long productId, BigDecimal price, int stock,
                                          LocalDateTime startAt, LocalDateTime endAt) {
        return createActivity(farmerId, false, productId, price, stock, startAt, endAt);
    }

    public SeckillActivity createActivity(Long actorId, boolean admin, Long productId, BigDecimal price, int stock,
                                          LocalDateTime startAt, LocalDateTime endAt) {
        Product product = productMapper.selectById(productId);
        if (product == null || (!admin && !product.getFarmerId().equals(actorId))) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (stock <= 0 || stock > product.getStock() || !endAt.isAfter(startAt)
                || price.signum() < 0) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_PUBLISHABLE);
        }
        SeckillActivity activity = SeckillActivity.draft(productId, product.getFarmerId(), price, stock, 1, startAt, endAt);
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

    public SeckillActivityView toView(SeckillActivity activity) {
        return new SeckillActivityView(activity.getId(), activity.getProductId(), activity.getFarmerId(),
                activity.getStatus(), activity.getSeckillPrice(), activity.getTotalStock(), activity.getLimitPerUser(),
                activity.getStartAt(), activity.getEndAt());
    }

    @org.springframework.transaction.annotation.Transactional
    public OrderView rush(Long activityId, Long userId, SeckillRequest request) {
        SeckillActivity activity = get(activityId);
        reserve(activityId, userId);
        Product product = productMapper.selectById(activity.getProductId());
        try {
            Order order = Order.createSeckill(nextOrderNo(), userId, activity.getFarmerId(), activity.getSeckillPrice(),
                    request.receiverName(), request.receiverPhone(), request.receiverAddress());
            orderMapper.insert(order);
            OrderItem item = OrderItem.create(order.getId(), product.getId(), product.getName(), product.getImageUrl(),
                    product.getOriginPlace(), activity.getSeckillPrice(), 1);
            orderItemMapper.insert(item);
            seckillOrderMapper.insert(SeckillOrder.create(activityId, order.getId(), userId));
            OrderItemView itemView = new OrderItemView(item.getId(), item.getProductId(), item.getProductName(),
                    item.getProductImageUrl(), item.getOriginPlace(), item.getUnitPrice(), item.getQuantity(), item.getSubtotal());
            return new OrderView(order.getId(), order.getOrderNo(), order.getBuyerId(), order.getFarmerId(),
                    order.getStatus(), order.getOrderType(), order.getTotalAmount(), order.getReceiverName(),
                    order.getReceiverPhone(), order.getReceiverAddress(), List.of(itemView));
        } catch (RuntimeException exception) {
            reservation.compensate(activityId, userId);
            throw new BusinessException(ErrorCode.SECKILL_ORDER_FAILED);
        }
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

    private String nextOrderNo() {
        return "SK" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now())
                + "%06d".formatted(ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
