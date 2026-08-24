package com.agromall.farmer.domain;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @TableName("farmer_profiles")
public class FarmerProfile {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId; private String realName; private String idCard; private String province;
    private String city; private String district; private String detailAddress; private String category;
    private String licenseNo; private FarmerApplicationStatus status; private String rejectReason;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}
