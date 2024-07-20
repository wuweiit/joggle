package com.wuweibi.bullet.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wuweibi.bullet.alias.CacheBlock;
import com.wuweibi.bullet.config.cache.RedisTemplateConfig;
import com.wuweibi.bullet.dashboard.domain.*;
import com.wuweibi.bullet.domain.vo.CountVO;
import com.wuweibi.bullet.mapper.CountMapper;
import com.wuweibi.bullet.mapper.DeviceMappingMapper;
import com.wuweibi.bullet.service.CountService;
import com.wuweibi.bullet.utils.BigDecimalUtils;
import com.wuweibi.bullet.utils.StringUtil;
import lombok.*;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.wuweibi.bullet.alias.CacheCode.DEVICE_MAPPING_STATISTICS_FLOW_TODAY;
import static com.wuweibi.bullet.alias.CacheCode.DEVICE_MAPPING_STATISTICS_LINK_TODAY;


@Service
public class CountServiceImpl implements CountService {

    @Resource
    private CountMapper countMapper;
    @Resource
    private DeviceMappingMapper deviceMappingMapper;

    @Resource(name = RedisTemplateConfig.BEAN_REDIS_TEMPLATE)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public CountVO getCountInfo() {
        return countMapper.selectCountInfo();
    }

    @Override
    public UserCountVO getUserCountInfo(Long userId) {
        UserFlowCountDTO userFlowCountDTO =  countMapper.selectUserCountInfo(userId);
        if (userFlowCountDTO == null) {
            return new UserCountVO();
        }
        UserCountVO userCountVO = new UserCountVO();
        BeanUtils.copyProperties(userFlowCountDTO, userCountVO);
        // 今日实时流量数据
        UserTodayFlowCountVO userTodayFlowCountVO = this.getUserTodayFow(userId, null);
        userCountVO.setTodayFlow(userTodayFlowCountVO.getTodayFlow());
        userCountVO.setTodayLink(userTodayFlowCountVO.getTodayLink());
        // 计算环比
        userCountVO.setTodayFlowOn(BigDecimalUtils
                .getChainRatio(userTodayFlowCountVO.getTodayFlow(), userFlowCountDTO.getTodayFlow2()));

        return userCountVO;
    }


    // 获取用户今日流量
    public UserTodayFlowCountVO getUserTodayFow(Long userId, Long deviceId){
        // TODO 支持按设备筛选实时数据
        // 数据库查询今日数据为0， 获取今日流量
        // 查询用户得maping 列表， 遍历redis
        List<Integer> idList = deviceMappingMapper.getMappingIdByUserId(userId);
        if(CollectionUtils.isEmpty(idList)){
            return new UserTodayFlowCountVO();
        }
        // redis 获取今日流量
        String date = DateUtil.format(new Date(), "yyyyMMdd");
        String keyBytes = String.format(DEVICE_MAPPING_STATISTICS_FLOW_TODAY, date);
        String keyLink = String.format(DEVICE_MAPPING_STATISTICS_LINK_TODAY, date);
        BoundHashOperations<String, Object, Object> keyBytesMap = redisTemplate.boundHashOps(keyBytes);
        BoundHashOperations<String, Object, Object> keyLinkMap = redisTemplate.boundHashOps(keyLink);

        Map<String, BigDecimal> todayData = idList.stream().flatMap(mappingId -> {
            Integer flowKb = (Integer) keyBytesMap.get(mappingId.toString());
            Integer linkNum = (Integer) keyLinkMap.get(mappingId.toString());

            return Stream.of(new TodayFlowInfo("flow",
                    new BigDecimal(flowKb == null ? 0 : flowKb)
                            .divide(BigDecimal.valueOf(1024))
            ), new TodayFlowInfo("link", new BigDecimal(flowKb == null ? 0 : linkNum)));
        }).collect(Collectors.groupingBy(TodayFlowInfo::getType,
                Collectors.mapping(TodayFlowInfo::getValue, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
        ));
        return new UserTodayFlowCountVO(todayData.get("flow").setScale(2, RoundingMode.HALF_UP), todayData.get("link").intValue());
    }


    @Data
    @AllArgsConstructor
    static class TodayFlowInfo{
        private String type;
        private BigDecimal value;
    }



    @Override
    public List<DeviceCountInfoVO> getUserDeviceRank(Long userId, Integer type) {
        if(Objects.equals(1, type)){
            // 查询用户设备清单和mappingId
            List<DeviceMappingInfoDTO> deviceMappingInfoDTOList = deviceMappingMapper.getDeviceMappingIdByUserId(userId);
            if(deviceMappingInfoDTOList.isEmpty()){
                return Collections.EMPTY_LIST;
            }
            // redis 获取今日流量
            String date = DateUtil.format(new Date(), "yyyyMMdd");
            String keyBytes = String.format(DEVICE_MAPPING_STATISTICS_FLOW_TODAY, date);
            BoundHashOperations<String, Object, Object> keyBytesMap = redisTemplate.boundHashOps(keyBytes);

            // 匹配流量数据
            deviceMappingInfoDTOList.forEach(item->{
                List<Integer> mappingIdList = StringUtil.splitInt(item.getMappingIds(),",");
                mappingIdList.forEach(id -> {
                    Integer flowKb = (Integer) keyBytesMap.get(id.toString());
                    BigDecimal val = new BigDecimal(flowKb == null ? 0 : flowKb)
                            .divide(BigDecimal.valueOf(1024));
                    item.setFlow(item.getFlow().add(val));
                });
            });
            return deviceMappingInfoDTOList.stream().filter(item->BigDecimal.ZERO.compareTo(item.getFlow()) < 0).map(item -> {
                DeviceCountInfoVO deviceCountInfoVO = new DeviceCountInfoVO();
                BeanUtils.copyProperties(item, deviceCountInfoVO);
                return deviceCountInfoVO;
            }).collect(Collectors.toList());
        }

        List<DeviceCountInfoVO> userCountVO = countMapper.selectUserDeviceRank(userId, type);


        return userCountVO;
    }

    @Override
    public List<DeviceDateItemVO> getUserDeviceTrend(Long userId, Long deviceId) {
        return countMapper.selectUserDeviceTrend(userId, deviceId);
    }

    @Override
    @Cacheable(cacheNames = CacheBlock.CACHE_HOME_TREND, key = "#day")
    public List<DeviceDateItemVO> getAllFlowTrend(int day) {
        return countMapper.selectAllFlowTrend(day);
    }


    @Cacheable(cacheNames = CacheBlock.CACHE_HOME_TREND_HOUR, key = "#hour")
    @Override
    public List<DeviceDateItemVO> getAllFlowTrendHour(int hour) {
        LocalDateTime now = LocalDateTime.now();
        String endDate = DateUtil.format(now.plusHours(-1), "yyyy-MM-dd");
        LocalDateTime startLocalDate = now.plusHours(-hour-1);
        String startDate = DateUtil.format(startLocalDate, "yyyy-MM-dd");

        // TODO  使用游标查询  改 流式查询
       Map<String,Optional<DataItem>>  userList = countMapper.selectAllFlowTrendHourStream(startDate, endDate).stream()
                .flatMap(dataMetricsHour -> {
                    String date = DateUtil.format(dataMetricsHour.getCreateDate(), "yyyy-MM-dd");
                    JSONObject data = (JSONObject) JSON.toJSON(dataMetricsHour);

                    List<DataItem> list = new ArrayList<>();

                    for(int i=0; i<24;i++){
                        String key = String.format("%02d", i);
                        String val = data.getString("h"+key);
                        String time = date + " "+ key;

                        BigDecimal link = BigDecimal.ZERO;
                        BigDecimal flowIn = BigDecimal.ZERO;
                        BigDecimal flowOut = BigDecimal.ZERO;
                        if (Objects.nonNull(val)) {
                            JSONObject itemData = new JSONObject(parse(val));
                            link = itemData.getBigDecimal("link");
                            flowIn = itemData.getBigDecimal("in");
                            flowOut = itemData.getBigDecimal("out");
                        }
                        DataItem dataItem = new DataItem(time, link, flowIn ,flowOut );
                        list.add(dataItem);
                    }


                    // 处理用户数据，例如转换或过滤
                    return Stream.of(list.toArray(new DataItem[]{}));
                }).collect(Collectors.groupingBy(DataItem::getTime,
                            Collectors.reducing(CountServiceImpl::mergeFlow)
                ));


        return userList.values().stream().map(dataItemOptional->{
            DataItem item = dataItemOptional.get();
            DeviceDateItemVO deviceDateItemVO = new DeviceDateItemVO();
            deviceDateItemVO.setTime(item.getTime());
            deviceDateItemVO.setFlowIn(item.getFlowIn());
            deviceDateItemVO.setFlowOut(item.getFlowOut());
            deviceDateItemVO.setLink(item.getLink());
            deviceDateItemVO.setFlow(item.getFlowIn().add(item.getFlowOut()));
            return deviceDateItemVO;
        }).collect(Collectors.toList());
    }

    // 累加方法
    private static DataItem mergeFlow(DataItem o, DataItem p) {
        o.setTime(p.getTime());
        o.setLink(o.getLink().add(p.getLink()));
        o.setFlowOut(o.getFlowOut().add(p.getFlowOut()));
        o.setFlowIn(o.getFlowIn().add(p.getFlowIn()));
        return o;
    }



    public static Map<String, Object> parse(String input) {
        Map<String, Object> result = new HashMap<>(3);
        String[] pairs = input.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                result.put(keyValue[0], keyValue[1]);
            }
        }
        return result;
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Setter
    @Getter
    public static class DataItem{
        private String time;
        private BigDecimal link = BigDecimal.ZERO;
        private BigDecimal flowIn = BigDecimal.ZERO;
        private BigDecimal flowOut = BigDecimal.ZERO;
    }

}
