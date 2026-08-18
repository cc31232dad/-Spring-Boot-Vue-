package com.agromall.usercenter.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(@Size(max = 64) String nickname, @Size(max = 64) String realName,
                                   @Email @Size(max = 128) String email, @Size(max = 500) String avatarUrl) {}
