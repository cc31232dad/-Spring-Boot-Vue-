package com.agromall.farmer.api;
import com.agromall.common.api.ApiResponse;
import com.agromall.farmer.application.FarmerOnboardingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth/farmer")
public class FarmerController {
    private final FarmerOnboardingService service;
    public FarmerController(FarmerOnboardingService service) { this.service=service; }
    @PostMapping("/apply") public ApiResponse<FarmerProfileView> apply(@Valid @RequestBody FarmerApplicationRequest request) { return ApiResponse.ok(service.apply(request)); }
}
