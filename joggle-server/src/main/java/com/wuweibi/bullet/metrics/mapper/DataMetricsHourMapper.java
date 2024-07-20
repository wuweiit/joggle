package com.wuweibi.bullet.metrics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuweibi.bullet.metrics.entity.DataMetricsHour;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * 数据收集(小时)(DataMetricsHour)表数据库访问层
 *
 * @author marker
 * @since 2024-07-17 18:02:16
 */
public interface DataMetricsHourMapper extends BaseMapper<DataMetricsHour> {

    /**
     * 根据时间和mappingId查询
     * @param date 日期间 yyyyMMdd
     * @param mappingId 映射id
     * @return
     */
    DataMetricsHour selectByMappingDate(@Param("date") Date date,@Param("mappingId") Long mappingId);
}
