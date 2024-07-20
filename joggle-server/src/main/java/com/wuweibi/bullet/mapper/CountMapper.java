package com.wuweibi.bullet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuweibi.bullet.dashboard.domain.DeviceCountInfoVO;
import com.wuweibi.bullet.dashboard.domain.DeviceDateItemVO;
import com.wuweibi.bullet.dashboard.domain.UserFlowCountDTO;
import com.wuweibi.bullet.domain.vo.CountVO;
import com.wuweibi.bullet.metrics.entity.DataMetricsHour;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
  *  Mapper 接口
 * </p>
 *
 * @author marker
 * @since 2017-12-09
 */
public interface CountMapper extends BaseMapper {


    /**
     * 统计首页情况
     * @return
     */
    CountVO selectCountInfo();

    /**
     * 统计用户的流量情况
     * @param userId 用户ID
     * @return
     */
    UserFlowCountDTO selectUserCountInfo(@Param("userId") Long userId);

    /**
     * 统计设备流量排行
     * @param userId 用户ID
     * @param type
     * @return
     */
    List<DeviceCountInfoVO> selectUserDeviceRank(@Param("userId") Long userId, @Param("type") Integer type);

    /**
     * 统计设备流量趋势
     * @param userId 用户id
     * @param deviceId 设备id
     * @return
     */
    List<DeviceDateItemVO> selectUserDeviceTrend(@Param("userId")Long userId, @Param("deviceId")Long deviceId);

    List<DeviceDateItemVO> selectAllFlowTrend(@Param("day") int day);

    List<DataMetricsHour> selectAllFlowTrendHourStream(String startDate, String endDate);
}
