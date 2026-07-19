package com.agromall.order.application;

import com.agromall.cart.application.CartService;
import com.agromall.cart.domain.CartItem;
import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import com.agromall.order.api.CheckoutRequest;
import com.agromall.order.api.OrderItemView;
import com.agromall.order.api.OrderView;
import com.agromall.order.domain.Order;
import com.agromall.order.domain.OrderItem;
import com.agromall.order.domain.OrderStatus;
import com.agromall.order.infrastructure.OrderItemMapper;
import com.agromall.order.infrastructure.OrderMapper;
import com.agromall.product.domain.Product;
import com.agromall.product.domain.ProductStatus;
import com.agromall.product.infrastructure.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final CartService cartService;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public OrderService(CartService cartService, ProductMapper productMapper, OrderMapper orderMapper,
                        OrderItemMapper orderItemMapper) {
        this.cartService = cartService;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Transactional
    public List<OrderView> checkout(Long buyerId, CheckoutRequest request) {
        List<CartItem> cartItems = cartService.getOwnedItemsForCheckout(buyerId, request.cartItemIds());
        if (cartItems.size() != request.cartItemIds().size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        Map<Long, Product> products = productsById(cartItems);
        for (CartItem item : cartItems) {
            Product product = products.get(item.getProductId());
            if (!ProductStatus.ON_SALE.name().equals(product.getStatus())) {
                throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE);
            }
            if (item.getQuantity() > product.getStock()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
        }

        Map<Long, List<CartItem>> itemsByFarmer = cartItems.stream()
                .collect(Collectors.groupingBy(item -> products.get(item.getProductId()).getFarmerId(),
                        LinkedHashMap::new, Collectors.toList()));
        List<OrderView> orders = itemsByFarmer.values().stream()
                .map(items -> createOrder(buyerId, request, items, products))
                .toList();
        cartService.deleteItems(buyerId, request.cartItemIds());
        return orders;
    }

    public List<OrderView> getMyOrders(Long buyerId) {
        return orderMapper.selectList(new LambdaQueryWrapper<Order>()
                        .eq(Order::getBuyerId, buyerId)
                        .orderByDesc(Order::getCreatedAt))
                .stream().map(this::toView).toList();
    }

    public OrderView getMyOrder(Long buyerId, Long orderId) {
        return toView(ownedOrder(buyerId, orderId));
    }

    @Transactional
    public OrderView cancel(Long buyerId, Long orderId) {
        Order order = ownedOrder(buyerId, orderId);
        if (orderMapper.markCancelledIfPending(orderId) != 1) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        List<OrderItem> items = orderItems(order.getId());
        for (OrderItem item : items) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED.name());
        return toView(order, items);
    }

    @Transactional
    public OrderView complete(Long buyerId, Long orderId) {
        Order order = ownedOrder(buyerId, orderId);
        if (!OrderStatus.SHIPPED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        order.setStatus(OrderStatus.COMPLETED.name());
        orderMapper.updateById(order);
        return toView(order);
    }

    private OrderView createOrder(Long buyerId, CheckoutRequest request, List<CartItem> cartItems,
                                  Map<Long, Product> products) {
        Long farmerId = products.get(cartItems.getFirst().getProductId()).getFarmerId();
        BigDecimal total = cartItems.stream()
                .map(item -> products.get(item.getProductId()).getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order = Order.create(nextOrderNo(), buyerId, farmerId, total, request.receiverName(),
                request.receiverPhone(), request.receiverAddress());
        orderMapper.insert(order);

        List<OrderItem> items = cartItems.stream().map(cartItem -> {
            Product product = products.get(cartItem.getProductId());
            OrderItem item = OrderItem.create(order.getId(), product.getId(), product.getName(), product.getImageUrl(),
                    product.getOriginPlace(), product.getPrice(), cartItem.getQuantity());
            orderItemMapper.insert(item);
            if (productMapper.deductStock(product.getId(), cartItem.getQuantity()) != 1) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
            return item;
        }).toList();
        return toView(order, items);
    }

    private Map<Long, Product> productsById(List<CartItem> cartItems) {
        Map<Long, Product> products = productMapper.selectBatchIds(cartItems.stream()
                        .map(CartItem::getProductId).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        if (products.size() != cartItems.stream().map(CartItem::getProductId).distinct().count()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return products;
    }

    private Order ownedOrder(Long buyerId, Long orderId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId).eq(Order::getBuyerId, buyerId));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private List<OrderItem> orderItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId).orderByAsc(OrderItem::getId));
    }

    private OrderView toView(Order order) {
        return toView(order, orderItems(order.getId()));
    }

    private OrderView toView(Order order, List<OrderItem> items) {
        List<OrderItemView> views = items.stream().map(item -> new OrderItemView(item.getId(), item.getProductId(),
                item.getProductName(), item.getProductImageUrl(), item.getOriginPlace(), item.getUnitPrice(),
                item.getQuantity(), item.getSubtotal())).toList();
        return new OrderView(order.getId(), order.getOrderNo(), order.getBuyerId(), order.getFarmerId(),
                order.getStatus(), order.getTotalAmount(), order.getReceiverName(), order.getReceiverPhone(),
                order.getReceiverAddress(), views);
    }

    private String nextOrderNo() {
        return "ORD" + ORDER_TIME_FORMAT.format(LocalDateTime.now())
                + "%06d".formatted(ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
