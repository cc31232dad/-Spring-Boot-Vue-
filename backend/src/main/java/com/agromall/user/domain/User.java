package com.agromall.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String nickname;
    private String realName;
    private String email;
    private String avatarUrl;
    private String passwordHash;
    private String phone;
    private Boolean enabled;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User(String username, String passwordHash, String phone) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.enabled = true;
        this.version = 0;
    }

    public static User create(String username, String passwordHash, String phone) {
        return new User(username, passwordHash, phone);
    }
}
