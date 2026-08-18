package com.agromall.usercenter.api;

public record AddressView(Long id, String name, String phone, String province, String city,
                          String district, String detail, boolean isDefault) {}
