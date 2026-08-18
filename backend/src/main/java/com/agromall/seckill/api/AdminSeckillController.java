package com.agromall.seckill.api;

import com.agromall.auth.security.JwtService;
import com.agromall.common.api.ApiResponse;
import com.agromall.seckill.application.SeckillService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/seckill")
public class AdminSeckillController {
    private final SeckillService service;

    public AdminSeckillController(SeckillService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<SeckillActivityView> create(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                                   @Valid @RequestBody CreateSeckillActivityRequest request) {
        return ApiResponse.ok(service.toView(service.createActivity(principal.userId(), principal.roles().contains("ADMIN"), request.productId(),
                request.seckillPrice(), request.totalStock(), request.startAt(), request.endAt())));
    }

    @PatchMapping("/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        service.publish(id);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/end")
    public ApiResponse<Void> end(@PathVariable Long id) {
        service.end(id);
        return ApiResponse.ok(null);
    }
}
