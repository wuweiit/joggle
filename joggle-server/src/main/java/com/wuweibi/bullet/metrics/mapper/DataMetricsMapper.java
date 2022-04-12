package com.wuweibi.bullet.metrics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wuweibi.bullet.metrics.entity.DataMetrics;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.Date;

/**
 * 数据收集(DataMetrics)表数据库访问层
 *
 * @author marker
 * @since 2021-11-07 14:17:52
 */
public interface DataMetricsMapper extends BaseMapper<DataMetrics> {



    boolean generateDayByTime(@Param("date") Date date);

}