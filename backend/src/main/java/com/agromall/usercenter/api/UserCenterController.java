package com.agromall.usercenter.api;

import com.agromall.auth.security.JwtService;
import com.agromall.common.api.ApiResponse;
import com.agromall.usercenter.application.UserCenterService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserCenterController {
    private final UserCenterService service;
    public UserCenterController(UserCenterService service) { this.service = service; }

    @GetMapping("/api/address")
    public ApiResponse<List<AddressView>> addresses(@AuthenticationPrincipal JwtService.JwtPrincipal p) { return ApiResponse.ok(service.listAddresses(p.userId())); }
    @PostMapping("/api/address")
    public ApiResponse<AddressView> addAddress(@AuthenticationPrincipal JwtService.JwtPrincipal p, @Valid @RequestBody AddressRequest r) { return ApiResponse.ok(service.createAddress(p.userId(), r)); }
    @PutMapping("/api/address/{id}")
    public ApiResponse<AddressView> updateAddress(@AuthenticationPrincipal JwtService.JwtPrincipal p, @PathVariable Long id, @Valid @RequestBody AddressRequest r) { return ApiResponse.ok(service.updateAddress(p.userId(), id, r)); }
    @DeleteMapping("/api/address/{id}")
    public ApiResponse<Void> deleteAddress(@AuthenticationPrincipal JwtService.JwtPrincipal p, @PathVariable Long id) { service.deleteAddress(p.userId(), id); return ApiResponse.ok(null); }
    @PutMapping("/api/address/{id}/default")
    public ApiResponse<AddressView> setDefault(@AuthenticationPrincipal JwtService.JwtPrincipal p, @PathVariable Long id) { return ApiResponse.ok(service.setDefaultAddress(p.userId(), id)); }

    @GetMapping("/api/favorites")
    public ApiResponse<List<FavoriteView>> favorites(@AuthenticationPrincipal JwtService.JwtPrincipal p) { return ApiResponse.ok(service.listFavorites(p.userId())); }
    @PostMapping("/api/favorites/{productId}")
    public ApiResponse<FavoriteView> addFavorite(@AuthenticationPrincipal JwtService.JwtPrincipal p, @PathVariable Long productId) { return ApiResponse.ok(service.addFavorite(p.userId(), productId)); }
    @DeleteMapping("/api/favorites/{productId}")
    public ApiResponse<Void> removeFavorite(@AuthenticationPrincipal JwtService.JwtPrincipal p, @PathVariable Long productId) { service.removeFavorite(p.userId(), productId); return ApiResponse.ok(null); }

    @GetMapping("/api/user/profile")
    public ApiResponse<ProfileView> profile(@AuthenticationPrincipal JwtService.JwtPrincipal p) { return ApiResponse.ok(service.profile(p.userId())); }
    @PutMapping("/api/user/profile")
    public ApiResponse<ProfileView> updateProfile(@AuthenticationPrincipal JwtService.JwtPrincipal p, @Valid @RequestBody ProfileUpdateRequest r) { return ApiResponse.ok(service.updateProfile(p.userId(), r)); }
    @PutMapping("/api/user/password")
    public ApiResponse<Void> updatePassword(@AuthenticationPrincipal JwtService.JwtPrincipal p, @Valid @RequestBody PasswordUpdateRequest r) { service.updatePassword(p.userId(), r); return ApiResponse.ok(null); }
    @PutMapping("/api/user/phone")
    public ApiResponse<ProfileView> updatePhone(@AuthenticationPrincipal JwtService.JwtPrincipal p, @Valid @RequestBody PhoneUpdateRequest r) { return ApiResponse.ok(service.updatePhone(p.userId(), r)); }
}
