package com.agromall.farmer.application;

import com.agromall.common.exception.BusinessException;
import com.agromall.common.exception.ErrorCode;
import com.agromall.farmer.api.FarmerApplicationRequest;
import com.agromall.farmer.api.FarmerProfileView;
import com.agromall.farmer.domain.*;
import com.agromall.farmer.infrastructure.FarmerAuditMapper;
import com.agromall.farmer.infrastructure.FarmerProfileMapper;
import com.agromall.user.domain.Role;
import com.agromall.user.domain.User;
import com.agromall.user.infrastructure.RoleMapper;
import com.agromall.user.infrastructure.UserMapper;
import com.agromall.user.infrastructure.UserRoleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FarmerOnboardingService {
    private final UserMapper userMapper; private final RoleMapper roleMapper; private final UserRoleMapper userRoleMapper;
    private final FarmerProfileMapper profileMapper; private final FarmerAuditMapper auditMapper;
    private final BCryptPasswordEncoder encoder;
    public FarmerOnboardingService(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                                   FarmerProfileMapper profileMapper, FarmerAuditMapper auditMapper,
                                   BCryptPasswordEncoder encoder) {
        this.userMapper=userMapper; this.roleMapper=roleMapper; this.userRoleMapper=userRoleMapper;
        this.profileMapper=profileMapper; this.auditMapper=auditMapper; this.encoder=encoder;
    }
    @Transactional
    public FarmerProfileView apply(FarmerApplicationRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, request.phone()));
        FarmerProfile existing = user == null ? null : profileMapper.selectOne(Wrappers.<FarmerProfile>lambdaQuery().eq(FarmerProfile::getUserId, user.getId()));
        if (existing != null && existing.getStatus() != FarmerApplicationStatus.REJECTED)
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        if (user == null) {
            user = User.create(request.phone(), encoder.encode(request.password()), request.phone());
            userMapper.insert(user);
            Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, "FARMER"));
            userRoleMapper.insert(user.getId(), role.getId());
        } else {
            user.setPasswordHash(encoder.encode(request.password())); userMapper.updateById(user);
        }
        FarmerProfile profile = existing == null ? new FarmerProfile() : existing; profile.setUserId(user.getId()); profile.setRealName(request.realName());
        profile.setIdCard(request.idCard()); profile.setProvince(request.province()); profile.setCity(request.city());
        profile.setDistrict(request.district()); profile.setDetailAddress(request.detailAddress()); profile.setCategory(request.category());
        profile.setLicenseNo(request.licenseNo()); profile.setStatus(FarmerApplicationStatus.PENDING); profile.setRejectReason(null);
        if (existing == null) profileMapper.insert(profile); else profileMapper.updateById(profile);
        FarmerAudit audit = new FarmerAudit(); audit.setFarmerId(profile.getId()); audit.setStatus(FarmerAuditStatus.PENDING); auditMapper.insert(audit);
        return view(profile, user);
    }
    public List<FarmerProfileView> listPending() { return profileMapper.selectByStatus(FarmerApplicationStatus.PENDING).stream().map(p -> view(p, userMapper.selectById(p.getUserId()))).toList(); }
    @Transactional public FarmerProfileView approve(Long adminId, Long id) { return decide(adminId,id,FarmerApplicationStatus.APPROVED,null); }
    @Transactional public FarmerProfileView reject(Long adminId, Long id, String reason) { return decide(adminId,id,FarmerApplicationStatus.REJECTED,reason.trim()); }
    private FarmerProfileView decide(Long adminId, Long id, FarmerApplicationStatus status, String reason) {
        FarmerProfile p = profileMapper.selectById(id); if (p == null) throw new BusinessException(ErrorCode.FARMER_PROFILE_NOT_FOUND);
        if (p.getStatus() != FarmerApplicationStatus.PENDING) throw new BusinessException(ErrorCode.FARMER_APPLICATION_INVALID);
        p.setStatus(status); p.setRejectReason(reason); profileMapper.updateById(p);
        FarmerAudit a = new FarmerAudit(); a.setFarmerId(id); a.setAdminId(adminId); a.setStatus(status == FarmerApplicationStatus.APPROVED ? FarmerAuditStatus.APPROVED : FarmerAuditStatus.REJECTED); a.setRemark(reason); auditMapper.insert(a);
        return view(p, userMapper.selectById(p.getUserId()));
    }
    private FarmerProfileView view(FarmerProfile p, User u) { return new FarmerProfileView(p.getId(),p.getUserId(),u.getPhone(),p.getRealName(),p.getIdCard(),p.getProvince(),p.getCity(),p.getDistrict(),p.getDetailAddress(),p.getCategory(),p.getLicenseNo(),p.getStatus(),p.getRejectReason()); }
}
