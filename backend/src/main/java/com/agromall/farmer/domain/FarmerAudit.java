package com.agromall.farmer.domain;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @TableName("farmer_audits")
public class FarmerAudit {
    @TableId(type = IdType.AUTO) private Long id;
    private Long farmerId; private Long adminId; private FarmerAuditStatus status; private String remark;
    private LocalDateTime createdAt;
}
