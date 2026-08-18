package com.agromall.seckill.api;

import com.agromall.auth.security.JwtService;
import com.agromall.common.api.ApiResponse;
import com.agromall.order.api.OrderView;
import com.agromall.seckill.application.SeckillService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {
    private final SeckillService service;

    public SeckillController(SeckillService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SeckillActivityView>> list() {
        return ApiResponse.ok(service.listPublished().stream().map(service::toView).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<SeckillActivityView> detail(@PathVariable Long id) {
        return ApiResponse.ok(service.toView(service.get(id)));
    }

    @PostMapping("/{id}/rush")
    public ApiResponse<OrderView> rush(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                      @PathVariable Long id, @Valid @RequestBody SeckillRequest request) {
        return ApiResponse.ok(service.rush(id, principal.userId(), request));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal JwtService.JwtPrincipal principal,
                                    @PathVariable Long id) {
        service.cancel(id, principal.userId());
        return ApiResponse.ok(null);
    }
}
