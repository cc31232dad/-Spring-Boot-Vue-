package com.agromall.usercenter.application;

import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import com.agromall.product.infrastructure.ProductMapper;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.UserMapper;
import com.agromall.usercenter.api.*;
import com.agromall.usercenter.domain.UserAddress;
import com.agromall.usercenter.domain.UserFavorite;
import com.agromall.usercenter.infrastructure.UserAddressMapper;
import com.agromall.usercenter.infrastructure.UserFavoriteMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserCenterService {
    private final UserMapper userMapper;
    private final UserAddressMapper addressMapper;
    private final UserFavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserCenterService(UserMapper userMapper, UserAddressMapper addressMapper,
                             UserFavoriteMapper favoriteMapper, ProductMapper productMapper,
                             BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.addressMapper = addressMapper;
        this.favoriteMapper = favoriteMapper;
        this.productMapper = productMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AddressView> listAddresses(Long userId) {
        return addressMapper.selectList(Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault).orderByDesc(UserAddress::getUpdatedAt))
                .stream().map(this::toAddressView).toList();
    }

    @Transactional
    public AddressView createAddress(Long userId, AddressRequest request) {
        UserAddress address = fromRequest(userId, request);
        boolean first = addressMapper.selectCount(Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId, userId)) == 0;
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || first;
        if (makeDefault) addressMapper.update(null, Wrappers.<UserAddress>lambdaUpdate().eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, false));
        address.setIsDefault(makeDefault);
        addressMapper.insert(address);
        return toAddressView(address);
    }

    @Transactional
    public AddressView updateAddress(Long userId, Long id, AddressRequest request) {
        UserAddress address = ownedAddress(userId, id);
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || Boolean.TRUE.equals(address.getIsDefault());
        if (makeDefault) addressMapper.update(null, Wrappers.<UserAddress>lambdaUpdate().eq(UserAddress::getUserId, userId)
                .ne(UserAddress::getId, id).set(UserAddress::getIsDefault, false));
        address.setName(request.name()); address.setPhone(request.phone()); address.setProvince(request.province());
        address.setCity(request.city()); address.setDistrict(request.district()); address.setDetail(request.detail());
        address.setIsDefault(makeDefault);
        addressMapper.updateById(address);
        return toAddressView(address);
    }

    @Transactional
    public void deleteAddress(Long userId, Long id) {
        UserAddress address = ownedAddress(userId, id);
        addressMapper.deleteById(id);
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            UserAddress replacement = addressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId, userId)
                    .orderByDesc(UserAddress::getUpdatedAt).last("LIMIT 1"));
            if (replacement != null) {
                replacement.setIsDefault(true);
                addressMapper.updateById(replacement);
            }
        }
    }

    @Transactional
    public AddressView setDefaultAddress(Long userId, Long id) {
        UserAddress address = ownedAddress(userId, id);
        addressMapper.update(null, Wrappers.<UserAddress>lambdaUpdate().eq(UserAddress::getUserId, userId)
                .ne(UserAddress::getId, id).set(UserAddress::getIsDefault, false));
        address.setIsDefault(true);
        addressMapper.updateById(address);
        return toAddressView(address);
    }

    public List<FavoriteView> listFavorites(Long userId) {
        return favoriteMapper.selectRowsByUserId(userId).stream().map(row -> new FavoriteView(row.id(), row.productId(),
                row.productName(), row.productPrice(), row.productStock(), row.productOriginPlace(), row.productImageUrl(),
                row.createdAt() == null ? null : row.createdAt().toString())).toList();
    }

    @Transactional
    public FavoriteView addFavorite(Long userId, Long productId) {
        if (productMapper.selectById(productId) == null) throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        UserFavorite favorite = favoriteMapper.selectOne(Wrappers.<UserFavorite>lambdaQuery().eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getProductId, productId));
        if (favorite == null) {
            favorite = new UserFavorite(); favorite.setUserId(userId); favorite.setProductId(productId); favoriteMapper.insert(favorite);
        }
        return listFavorites(userId).stream().filter(item -> item.productId().equals(productId)).findFirst().orElseThrow();
    }

    @Transactional
    public void removeFavorite(Long userId, Long productId) {
        favoriteMapper.delete(Wrappers.<UserFavorite>lambdaQuery().eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getProductId, productId));
    }

    public ProfileView profile(Long userId) { return toProfile(user(userId)); }

    @Transactional
    public ProfileView updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = user(userId);
        if (request.nickname() != null) user.setNickname(request.nickname());
        if (request.realName() != null) user.setRealName(request.realName());
        if (request.email() != null) user.setEmail(request.email());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        userMapper.updateById(user);
        return toProfile(user);
    }

    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = user(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) throw new BusinessException(ErrorCode.CURRENT_PASSWORD_INVALID);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    @Transactional
    public ProfileView updatePhone(Long userId, PhoneUpdateRequest request) {
        User user = user(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) throw new BusinessException(ErrorCode.CURRENT_PASSWORD_INVALID);
        User existing = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, request.phone()).ne(User::getId, userId));
        if (existing != null) throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        user.setPhone(request.phone()); userMapper.updateById(user);
        return toProfile(user);
    }

    private UserAddress fromRequest(Long userId, AddressRequest request) {
        UserAddress address = new UserAddress(); address.setUserId(userId); address.setName(request.name()); address.setPhone(request.phone());
        address.setProvince(request.province()); address.setCity(request.city()); address.setDistrict(request.district()); address.setDetail(request.detail()); return address;
    }
    private UserAddress ownedAddress(Long userId, Long id) {
        UserAddress address = addressMapper.selectOne(Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getId, id).eq(UserAddress::getUserId, userId));
        if (address == null) throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND); return address;
    }
    private User user(Long id) { User user = userMapper.selectById(id); if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND); return user; }
    private AddressView toAddressView(UserAddress a) { return new AddressView(a.getId(), a.getName(), a.getPhone(), a.getProvince(), a.getCity(), a.getDistrict(), a.getDetail(), Boolean.TRUE.equals(a.getIsDefault())); }
    private ProfileView toProfile(User u) { return new ProfileView(u.getId(), u.getUsername(), u.getNickname(), u.getRealName(), u.getPhone(), u.getEmail(), u.getAvatarUrl()); }
}
