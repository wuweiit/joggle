package com.wuweibi.bullet.metrics.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wuweibi.bullet.business.DeviceBiz;
import com.wuweibi.bullet.config.cache.RedisTemplateConfig;
import com.wuweibi.bullet.config.properties.JoggleProperties;
import com.wuweibi.bullet.device.domain.DeviceDetail;
import com.wuweibi.bullet.device.entity.ServerTunnel;
import com.wuweibi.bullet.device.service.ServerTunnelService;
import com.wuweibi.bullet.entity.DeviceMapping;
import com.wuweibi.bullet.entity.api.R;
import com.wuweibi.bullet.exception.type.SystemErrorType;
import com.wuweibi.bullet.flow.entity.UserFlow;
import com.wuweibi.bullet.flow.service.UserFlowService;
import com.wuweibi.bullet.mapper.DeviceMappingMapper;
import com.wuweibi.bullet.metrics.domain.DataMetricsDTO;
import com.wuweibi.bullet.metrics.domain.DataMetricsHourSettleDTO;
import com.wuweibi.bullet.metrics.domain.DataMetricsListVO;
import com.wuweibi.bullet.metrics.domain.DataMetricsParam;
import com.wuweibi.bullet.metrics.entity.DataMetrics;
import com.wuweibi.bullet.metrics.entity.DataMetricsHour;
import com.wuweibi.bullet.metrics.mapper.DataMetricsHourMapper;
import com.wuweibi.bullet.metrics.mapper.DataMetricsMapper;
import com.wuweibi.bullet.metrics.service.DataMetricsService;
import com.wuweibi.bullet.res.service.UserPackageService;
import com.wuweibi.bullet.service.DeviceService;
import com.wuweibi.bullet.utils.DateTimeUtil;
import com.wuweibi.bullet.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.wuweibi.bullet.alias.CacheCode.*;

/**
 * 数据收集(DataMetrics)表服务实现类
 *
 * @author marker
 * @since 2021-11-07 14:17:49
 */
@Slf4j
@Service
public class DataMetricsServiceImpl extends ServiceImpl<DataMetricsMapper, DataMetrics> implements DataMetricsService {

    @Override
    public boolean generateDayByTime(Date date) {
        // 不支持今日统计
        if (DateUtils.truncatedCompareTo(date, new Date(), Calendar.DATE) == 0) {
            throw new RuntimeException("不支持当日统计");
        }
        return this.baseMapper.generateDayByTime(date);
    }

    @Resource
    private DeviceService deviceService;
    @Resource
    private ServerTunnelService serverTunnelService;

    @Resource
    private UserFlowService userFlowService;

    @Resource
    private JoggleProperties joggleProperties;

    @Resource
    private DeviceBiz deviceBiz;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 根据1TB流量推算出来与阿里云之间的流量差异得出的误差因子
     * joggle 的 时间范围内  总流量除以总连接数 = 每个链接的平均
     * 2023年07月24日 到 2023年10月18日 跑完1TB流量
     * joggle 统计流量 939GB 相差85GB 除以总连接数 平均每个链接需要补 30kb 流量。
     *
     */
    private static final long FLOW_ERROR_FACTOR = 30 * 1024;

    @Resource(name = RedisTemplateConfig.BEAN_REDIS_TEMPLATE)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public R uploadData(DataMetricsDTO dataMetrics) {
        String deviceNo = dataMetrics.getDeviceNo();
        log.debug("流量deviceNo={}->{}", deviceNo, dataMetrics.toString());
        DeviceDetail deviceDetail = deviceService.getDetailByDeviceNo(deviceNo);
        if (deviceDetail == null) {
            log.warn("上报的设备不存在, 上报:{}", JSON.toJSONString(dataMetrics));
            return R.fail(SystemErrorType.DEVICE_NOT_EXIST);
        }

        ServerTunnel serverTunnel = serverTunnelService.getById(deviceDetail.getServerTunnelId());
        if (serverTunnel == null) {
            log.warn("上报的通道服务器不存在, 上报:{}", JSON.toJSONString(dataMetrics));
            return R.fail(SystemErrorType.DEVICE_NOT_EXIST);
        }


        DataMetrics entity = new DataMetrics();
        BeanUtils.copyProperties(dataMetrics, entity);
        entity.setCreateTime(new Date());
        entity.setServerTunnelId(deviceDetail.getServerTunnelId());
        entity.setDeviceId(deviceDetail.getId());
        entity.setUserId(deviceDetail.getUserId());
        entity.setOpenTime(new Date(dataMetrics.getOpenTime()));
        entity.setCloseTime(new Date(dataMetrics.getCloseTime()));
        entity.setDuration(dataMetrics.getCloseTime() - dataMetrics.getOpenTime());
        entity.setRemoteAddr(dataMetrics.getRemoteAddr());
        entity.setBytesIn(dataMetrics.getBytesIn() + FLOW_ERROR_FACTOR / 2);
        entity.setBytesOut(dataMetrics.getBytesOut() + FLOW_ERROR_FACTOR / 2);

        Long userId = deviceDetail.getUserId();

        synchronized (userId) {
            return transactionTemplate.execute(e -> {
                this.save(entity);

                // 流量in out 整合
                long bytes = entity.getBytesIn() + entity.getBytesOut();
                long byteKb = bytes / 1024;
                if (byteKb <= 0) {
                    byteKb = 1; // 至少消耗1KB
                }

                // 今日流量缓存 原子累加
                String date = DateUtil.format(new Date(), "yyyyMMdd");
                String keyBytes = String.format(DEVICE_MAPPING_STATISTICS_FLOW_TODAY, date);
                String keyLink = String.format(DEVICE_MAPPING_STATISTICS_LINK_TODAY, date);
                redisTemplate.opsForHash().increment(keyBytes, String.valueOf(entity.getMappingId()), byteKb);
                redisTemplate.opsForHash().increment(keyLink, String.valueOf(entity.getMappingId()), 1);
                if (0 > redisTemplate.getExpire(keyLink)) {
                    redisTemplate.expire(keyBytes, 2, TimeUnit.DAYS);
                    redisTemplate.expire(keyLink, 2, TimeUnit.DAYS);
                }
                // 小时级别数据统计
                String dateHour = DateUtil.format(new Date(), "yyyyMMddHH");
                String keyHourLink = String.format(DEVICE_MAPPING_STATISTICS_LINK_HOUR, dateHour);
                redisTemplate.opsForHash().increment(keyHourLink, String.valueOf(entity.getMappingId()), 1);
                String keyHourFlowIn = String.format(DEVICE_MAPPING_STATISTICS_FLOW_IN_HOUR, dateHour);
                redisTemplate.opsForHash().increment(keyHourFlowIn, String.valueOf(entity.getMappingId()), entity.getBytesIn());
                String keyHourFlowOut = String.format(DEVICE_MAPPING_STATISTICS_FLOW_OUT_HOUR, dateHour);
                redisTemplate.opsForHash().increment(keyHourFlowOut, String.valueOf(entity.getMappingId()), entity.getBytesOut());
                if (0 > redisTemplate.getExpire(keyHourLink)) {
                    redisTemplate.expire(keyHourFlowIn, 2, TimeUnit.DAYS);
                    redisTemplate.expire(keyHourFlowOut, 2, TimeUnit.DAYS);
                    redisTemplate.expire(keyHourLink, 2, TimeUnit.DAYS);
                }

                // 判断是否扣取流量
                if (!Objects.equals(1, serverTunnel.getEnableFlow())) {
                    return R.ok();
                }

                // 优先扣套餐流量 (带有保护，不支持负数)
                boolean status = userPackageService.updateFLow(userId, -byteKb);
                if (status) {
                    return R.ok();
                }
                // 套餐流量扣失败了，扣充值流量（没有保护只有成功）
                status = userFlowService.updateFLow(userId, -byteKb);
                if (!status) { // 后面判断基本不走
                    log.warn("流量扣取失败 userId={}", userId);
                    return R.fail(SystemErrorType.FLOW_IS_PAY_FAIL);
                }
                UserFlow userFlow = userFlowService.getUserFlowAndPackageFlow(userId); // 套餐流量和充值流量
                if (userFlow.getFlow() < -(1000)) { // 如果流量超出，关闭映射
                    // 由于用户没有流量了，默认关闭所有映射
                    deviceBiz.closeAllMappingByUserId(userId);
                }
                return R.ok();
            });
        }
    }
    @Resource
    private UserPackageService userPackageService;

    @Override
    public Page<DataMetricsListVO> getList(Page<DataMetrics> page, DataMetricsParam params) {
        return this.baseMapper.selectUserList(page, params);
    }

    @Override
    public void dataMetricsHourSettle(DataMetricsHourSettleDTO dataMetrics) {
        String currentDateHour = DateUtil.format(dataMetrics.getDate(), "yyyyMMddHH");
        log.info("结算流量小时级流量数据，当前:{}", currentDateHour);

        String keyHourLink = String.format(DEVICE_MAPPING_STATISTICS_LINK_HOUR, currentDateHour);
//        redisTemplate.opsForHash().getOperations().opsForHash() keyHourLink, String.valueOf(entity.getMappingId()), 1);
        String hourIndex = String.format("h%02d", Integer.valueOf(currentDateHour.substring(8,10)));
        String createDateStr = currentDateHour.substring(0,8);
        Date createDate = DateUtil.parse(createDateStr, "yyyyMMdd");

        BoundHashOperations<String, Object, Object> keyLinkHourMap = redisTemplate.boundHashOps(keyHourLink);
        Set<Map.Entry<Object, Object>> entrySet =  keyLinkHourMap.entries().entrySet();
        String finalCurrentDateHour = currentDateHour;
        entrySet.forEach(entry->{
            Long mappingId = Long.valueOf((String) entry.getKey());
            DeviceMapping deviceMapping = deviceMappingMapper.selectById(mappingId);
            if (Objects.isNull(deviceMapping)) {
                log.warn("mapping not found lose data:{}", JSON.toJSONString(dataMetrics));
                return;
            }

            DataMetricsHour dataMetricsHour = new DataMetricsHour();
            dataMetricsHour.setUserId(deviceMapping.getUserId());
            dataMetricsHour.setServerTunnelId(deviceMapping.getServerTunnelId());
            dataMetricsHour.setDeviceId(deviceMapping.getDeviceId());
            dataMetricsHour.setMappingId(deviceMapping.getId());
            dataMetricsHour.setCreateDate(createDate);
            dataMetricsHour.setCreateTime(new Date());

            Integer link = (Integer)entry.getValue();

            String flowIn = getFlowValue(DEVICE_MAPPING_STATISTICS_FLOW_IN_HOUR, finalCurrentDateHour, mappingId);
            String flowOut = getFlowValue(DEVICE_MAPPING_STATISTICS_FLOW_OUT_HOUR, finalCurrentDateHour, mappingId);

            String hourValue = String.format("link:%s,in:%s,out:%s", link, flowIn,flowOut);

            DataMetricsHour dataMetricsHour1 = dataMetricsHourMapper.selectByMappingDate(createDate, mappingId);
            if (null == dataMetricsHour1) {
                try {
                    Field hourField = dataMetricsHour.getClass().getDeclaredField(hourIndex);
                    hourField.setAccessible(true);
                    hourField.set(dataMetricsHour, hourValue);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                dataMetricsHourMapper.insert(dataMetricsHour);
            } else {
                try {
                    Field hourField = dataMetricsHour.getClass().getDeclaredField(hourIndex);
                    hourField.setAccessible(true);
                    hourField.set(dataMetricsHour1, hourValue);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                dataMetricsHourMapper.updateById(dataMetricsHour1);
            }
        });

    }


    /**
     * 每天凌晨0.30执行昨日数据
     *
     * @return
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void generateDayByTimeYesterday() {
        log.info("处理昨日流量数据。。。。");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        Date date = calendar.getTime();
        this.baseMapper.generateDayByTime(date);
    }

    @Resource
    private DeviceMappingMapper deviceMappingMapper;

    @Resource
    private DataMetricsHourMapper dataMetricsHourMapper;





    /**
     * 每1小时执行一次结算
     *
     * @return
     */
    @Scheduled(cron = "1 0 * * * ?")
    public void settleHourFlow() {
        String currentDateHour = (String) redisTemplate.opsForValue().get(DEVICE_MAPPING_STATISTICS_FLOW_HOUR_CURRENT);
        LocalDateTime currentLocalDateHour = DateUtil.parseLocalDateTime(currentDateHour, "yyyyMMddHH");
        if (StringUtil.isBlank(currentDateHour)) {
            currentDateHour = DateUtil.format(new Date(), "yyyyMMddHH");
            redisTemplate.opsForValue().set(DEVICE_MAPPING_STATISTICS_FLOW_HOUR_CURRENT, currentDateHour);
            return;
        }
        // 判断间隔小时数，多了就需要补偿
        String todayDateHour = DateUtil.format(new Date(), "yyyyMMddHH");
        LocalDateTime todayLocalDateHour = DateUtil.parseLocalDateTime(todayDateHour, "yyyyMMddHH");
        // 计算小时差
        int hours = DateTimeUtil.getHourDiff(currentLocalDateHour, todayLocalDateHour);
        if (hours <= 0) {
            return;
        }
        for (int i =0 ; i < (hours); i++){
            LocalDateTime cur = currentLocalDateHour.plusHours(i);
            // 将LocalDateTime转换为Date
            Date date = DateUtil.date(cur.atZone(ZoneId.systemDefault()).toInstant());
            DataMetricsHourSettleDTO dataMetricsHourSettleDTO = new DataMetricsHourSettleDTO();
            dataMetricsHourSettleDTO.setDate(date);
            this.dataMetricsHourSettle(dataMetricsHourSettleDTO);
        }

        redisTemplate.opsForValue().set(DEVICE_MAPPING_STATISTICS_FLOW_HOUR_CURRENT, todayDateHour);
    }



    private String getFlowValue(String keyFormat, String currentDateHour, Long mappingId) {
        String keyHour = String.format(keyFormat, currentDateHour);
        return (Objects.requireNonNull(redisTemplate.opsForHash().get(keyHour, String.valueOf(mappingId)))).toString();
    }
}
