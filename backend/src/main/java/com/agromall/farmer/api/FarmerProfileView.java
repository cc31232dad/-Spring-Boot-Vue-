package com.agromall.farmer.api;
import com.agromall.farmer.domain.FarmerApplicationStatus;
public record FarmerProfileView(Long id, Long userId, String phone, String realName, String idCard,
                                String province, String city, String district, String detailAddress,
                                String category, String licenseNo, FarmerApplicationStatus status,
                                String rejectReason) { }
