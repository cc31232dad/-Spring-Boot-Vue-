package com.agromall.farmer.infrastructure;
import com.agromall.farmer.domain.FarmerApplicationStatus;
import com.agromall.farmer.domain.FarmerProfile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
@Mapper public interface FarmerProfileMapper extends BaseMapper<FarmerProfile> {
    @Select("SELECT * FROM farmer_profiles WHERE status = #{status} ORDER BY created_at ASC")
    List<FarmerProfile> selectByStatus(FarmerApplicationStatus status);
}
