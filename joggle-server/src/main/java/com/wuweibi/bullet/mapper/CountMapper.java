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


    /**
     * 获取最近day天的天级别数据
     * @param day 天数
     * @return
     */
    List<DeviceDateItemVO> selectAllFlowTrend(@Param("day") int day);


    /**
     * 获取指定时间段的小时级别数据
     * @param startDate 开始时间 yyyy-MM-dd
     * @param endDate 结束时间 yyyy-MM-dd
     * @return
     */
    List<DataMetricsHour> selectAllFlowTrendHourStream(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
