package com.agromall.cart.application;

import com.agromall.cart.api.AddCartItemRequest;
import com.agromall.cart.api.CartItemView;
import com.agromall.cart.api.CartView;
import com.agromall.cart.api.UpdateCartItemRequest;
import com.agromall.cart.domain.CartItem;
import com.agromall.cart.infrastructure.CartItemMapper;
import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import com.agromall.product.domain.Product;
import com.agromall.product.domain.ProductStatus;
import com.agromall.product.infrastructure.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
    }

    public CartView getCart(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByAsc(CartItem::getCreatedAt));
        if (items.isEmpty()) {
            return new CartView(List.of(), BigDecimal.ZERO);
        }
        Map<Long, Product> products = productMapper.selectBatchIds(items.stream()
                        .map(CartItem::getProductId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        List<CartItemView> views = items.stream()
                .filter(item -> products.containsKey(item.getProductId()))
                .map(item -> toView(item, products.get(item.getProductId())))
                .toList();
        BigDecimal total = views.stream().map(CartItemView::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartView(views, total);
    }

    @Transactional
    public CartView addItem(Long userId, AddCartItemRequest request) {
        Product product = availableProduct(request.productId());
        CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, request.productId()));
        int quantity = request.quantity() + (item == null ? 0 : item.getQuantity());
        assertSufficientStock(product, quantity);
        if (item == null) {
            cartItemMapper.insert(CartItem.create(userId, request.productId(), quantity));
        } else {
            item.setQuantity(quantity);
            cartItemMapper.updateById(item);
        }
        return getCart(userId);
    }

    @Transactional
    public CartView updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        CartItem item = ownedItem(userId, itemId);
        Product product = availableProduct(item.getProductId());
        assertSufficientStock(product, request.quantity());
        item.setQuantity(request.quantity());
        cartItemMapper.updateById(item);
        return getCart(userId);
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {
        cartItemMapper.deleteById(ownedItem(userId, itemId));
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
    }

    public List<CartItem> getOwnedItems(Long userId, List<Long> itemIds) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .in(CartItem::getId, itemIds));
        if (items.size() != itemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return items;
    }

    @Transactional
    public void deleteItems(Long userId, List<Long> itemIds) {
        getOwnedItems(userId, itemIds);
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .in(CartItem::getId, itemIds));
    }

    private CartItem ownedItem(Long userId, Long itemId) {
        CartItem item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getId, itemId)
                .eq(CartItem::getUserId, userId));
        if (item == null) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return item;
    }

    private Product availableProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!ProductStatus.ON_SALE.name().equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE);
        }
        return product;
    }

    private void assertSufficientStock(Product product, int quantity) {
        if (quantity > product.getStock()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private CartItemView toView(CartItem item, Product product) {
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemView(item.getId(), product.getId(), product.getName(), product.getPrice(),
                item.getQuantity(), product.getStock(), product.getOriginPlace(), product.getImageUrl(),
                product.getFarmerId(), subtotal);
    }
}
