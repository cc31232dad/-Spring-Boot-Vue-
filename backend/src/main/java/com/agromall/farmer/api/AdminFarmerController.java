package com.agromall.farmer.api;
import com.agromall.auth.security.JwtService;
import com.agromall.common.api.ApiResponse;
import com.agromall.farmer.application.FarmerOnboardingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/admin/farmers")
public class AdminFarmerController {
    private final FarmerOnboardingService service;
    public AdminFarmerController(FarmerOnboardingService service) { this.service=service; }
    @GetMapping public ApiResponse<List<FarmerProfileView>> list() { return ApiResponse.ok(service.listPending()); }
    @PostMapping("/{id}/approve") public ApiResponse<FarmerProfileView> approve(@AuthenticationPrincipal JwtService.JwtPrincipal p,@PathVariable Long id){return ApiResponse.ok(service.approve(p.userId(),id));}
    @PostMapping("/{id}/reject") public ApiResponse<FarmerProfileView> reject(@AuthenticationPrincipal JwtService.JwtPrincipal p,@PathVariable Long id,@Valid @RequestBody FarmerRejectRequest r){return ApiResponse.ok(service.reject(p.userId(),id,r.reason()));}
}
