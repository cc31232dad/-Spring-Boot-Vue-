package com.agromall.usercenter.api;

public record ProfileView(Long id, String username, String nickname, String realName, String phone,
                          String email, String avatarUrl) {}
