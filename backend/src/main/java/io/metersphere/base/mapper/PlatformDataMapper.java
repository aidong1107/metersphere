package io.metersphere.base.mapper;

import io.metersphere.base.domain.PlatformData;
import io.metersphere.base.domain.PlatformDataExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface PlatformDataMapper {
    long countByExample(PlatformDataExample example);

    int deleteByExample(PlatformDataExample example);

    int insert(PlatformData record);

    int insertSelective(PlatformData record);

    List<PlatformData> selectByExampleWithBLOBs(PlatformDataExample example);

    List<PlatformData> selectByExample(PlatformDataExample example);

    PlatformData selectByPlatformId(@Param("platformId") String platformId);

    int updateByExampleSelective(@Param("record") PlatformData record, @Param("example") PlatformDataExample example);

    int updateByExampleWithBLOBs(@Param("record") PlatformData record, @Param("example") PlatformDataExample example);

    int updateByExample(@Param("record") PlatformData record, @Param("example") PlatformDataExample example);

    int updateDataByPlatformId(@Param("platformId") String platformId, @Param("data") String data);
}
