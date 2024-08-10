package com.wuweibi.bullet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wuweibi.bullet.conn.WebsocketPool;
import com.wuweibi.bullet.device.contrast.DeviceOnlineStatus;
import com.wuweibi.bullet.device.domain.DeviceDetail;
import com.wuweibi.bullet.device.domain.dto.DeviceOnlineInfoDTO;
import com.wuweibi.bullet.device.entity.DeviceOnlineLog;
import com.wuweibi.bullet.device.entity.ServerTunnel;
import com.wuweibi.bullet.device.service.DeviceOnlineLogService;
import com.wuweibi.bullet.device.service.ServerTunnelService;
import com.wuweibi.bullet.entity.DeviceOnline;
import com.wuweibi.bullet.mapper.DeviceMapper;
import com.wuweibi.bullet.mapper.DeviceOnlineMapper;
import com.wuweibi.bullet.protocol.MsgGetDeviceStatus;
import com.wuweibi.bullet.service.DeviceOnlineService;
import com.wuweibi.bullet.service.DeviceService;
import com.wuweibi.bullet.service.UserService;
import com.wuweibi.bullet.system.biz.NotifyBiz;
import com.wuweibi.bullet.websocket.Bullet3Annotation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author marker
 * @since 2017-12-09
 */
@Slf4j
@Service
public class DeviceOnlineServiceImpl extends ServiceImpl<DeviceOnlineMapper, DeviceOnline> implements DeviceOnlineService {

    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DeviceOnlineLogService deviceOnlineLogService;


    @Override
    @Transactional
    public void saveOrUpdateOnline(String deviceNo, String ip, String mac, String clientVersion) {

        QueryWrapper ew = new QueryWrapper();
        ew.eq("deviceNo", deviceNo);

        DeviceOnline deviceOnline = this.baseMapper.selectOne(ew);
        if (deviceOnline == null) {
            deviceOnline = new DeviceOnline();
            deviceOnline.setDeviceNo(deviceNo);
            deviceOnline.setStatus(1);// 等待被绑定（在线)
            deviceOnline.setUpdateTime(new Date());
            deviceOnline.setIntranetIp(ip); // ip
            deviceOnline.setMacAddr(mac); // mac
            deviceOnline.setClientVersion(clientVersion);
            this.baseMapper.insert(deviceOnline);
        } else {
            deviceOnline.setDeviceNo(deviceNo);
            deviceOnline.setStatus(1);// 等待被绑定（在线)
            deviceOnline.setUpdateTime(new Date());
            deviceOnline.setIntranetIp(ip); // ip
            deviceOnline.setMacAddr(mac); // mac
            deviceOnline.setClientVersion(clientVersion);
            this.baseMapper.updateById(deviceOnline);
        }

    }

    @Override
    public void updateOutLine(String deviceId) {
        QueryWrapper ew = new QueryWrapper();
        ew.eq("deviceNo", deviceId);

        DeviceOnline deviceOnline = new DeviceOnline();
        deviceOnline.setDeviceNo(deviceId);
        deviceOnline.setStatus(-1);// 下线后的设备部可绑定
        deviceOnline.setUpdateTime(new Date());
        this.baseMapper.update(deviceOnline, ew);
    }

    @Override
    public DeviceOnline selectByDeviceNo(String deviceNo) {
        QueryWrapper ew = new QueryWrapper();
        ew.eq("deviceNo", deviceNo);
        return this.baseMapper.selectOne(ew);
    }

    @Override
    public boolean existsOnline(String deviceNo) {

        QueryWrapper ew = new QueryWrapper();
        ew.eq("deviceNo", deviceNo);
        ew.eq("status", 1);

        Long count = this.baseMapper.selectCount(ew);
        return count > 0;
    }

    @Override
    public void allDownNow() {
        this.baseMapper.updateStatusDown();
    }

    @Override
    @Transactional
    public boolean saveOrUpdate(DeviceOnlineInfoDTO deviceInfo) {
        String deviceNo = deviceInfo.getDeviceNo();
        DeviceOnline deviceOnline = this.baseMapper.selectOne(Wrappers.<DeviceOnline>lambdaQuery()
                .eq(DeviceOnline::getDeviceNo, deviceNo));
        if (deviceOnline == null) {
            deviceOnline = new DeviceOnline();
            deviceOnline.setDeviceNo(deviceNo);
            deviceOnline.setStatus(1);// 等待被绑定（在线)
        }
        deviceOnline.setServerTunnelId(deviceInfo.getServerTunnelId());
        deviceOnline.setPublicIp(deviceInfo.getPublicIp());
        deviceOnline.setIntranetIp(deviceInfo.getIntranetIp());
        deviceOnline.setMacAddr(deviceInfo.getMacAddr());
        deviceOnline.setClientVersion(deviceInfo.getClientVersion());
        deviceOnline.setOs(deviceInfo.getOs());
        deviceOnline.setArch(deviceInfo.getArch());
        deviceOnline.setUpdateTime(new Date());

        if (deviceOnline.getId() != null) {
            deviceOnline.setStatus(1); // 更新一般是上线才会更新
            this.baseMapper.updateById(deviceOnline);
        } else {// save
            this.baseMapper.insert(deviceOnline);
        }
        return true;
    }


    @Override
    public DeviceOnline getByDeviceNo(String deviceId) {
        return this.baseMapper.selectOne(Wrappers.<DeviceOnline>lambdaQuery()
                .eq(DeviceOnline::getDeviceNo, deviceId)
                .eq(DeviceOnline::getStatus, DeviceOnlineStatus.ONLINE.status)
                .orderByDesc(DeviceOnline::getUpdateTime));
    }

    @Resource
    private WebsocketPool websocketPool;

    @Resource
    private ServerTunnelService serverTunnelService;

    /**
     * 一般初始化启动的时候调用该方法得到每个设备的状态
     *
     * @return
     */
    @Override
    public boolean checkDeviceStatus() {
        List<ServerTunnel> list = serverTunnelService.getListEnable();
        list.forEach(item -> {
            log.debug("[init] check server[{}] {}[{}]", item.getId(), item.getName(), item.getServerAddr());
            Bullet3Annotation annotation = websocketPool.getByTunnelId(item.getId());
            if (annotation == null) {
                log.debug("[init] check server[{}] not online", item.getId());
            }
            if (annotation != null) {
                MsgGetDeviceStatus msg = new MsgGetDeviceStatus();
                annotation.sendMessageToServer(msg);
                log.debug("[init] check server[{}] ok [GetDeviceStatus]", item.getId());
            }
        });
        return true;
    }

    @Resource
    private DeviceService deviceService;

    @Override
    public boolean updateDeviceStatus(String deviceNo, int status) {
        DeviceDetail deviceDetail = this.deviceService.getDetailByDeviceNo(deviceNo);
        if (deviceDetail == null) {
            return false;
        }
        this.baseMapper.updateDeviceStatus(deviceNo, status);

        DeviceOnlineLog deviceOnlineLog = new DeviceOnlineLog();
        deviceOnlineLog.setUserId(deviceDetail.getUserId());
        deviceOnlineLog.setDeviceId(deviceDetail.getId());
        deviceOnlineLog.setDeviceName(deviceDetail.getName());
        deviceOnlineLog.setMacAddr(deviceDetail.getMacAddr());
        deviceOnlineLog.setIntranetIp(deviceDetail.getIntranetIp());
        deviceOnlineLog.setPublicIp(deviceDetail.getPublicIp());
        deviceOnlineLog.setStatus(status);
        deviceOnlineLog.setServerTunnelId(deviceDetail.getServerTunnelId());
        deviceOnlineLog.setArch(deviceDetail.getArch());
        deviceOnlineLog.setOs(deviceDetail.getOs());
        deviceOnlineLog.setCreateTime(new Date());
        deviceOnlineLog.setUpdateTime(new Date());
        this.deviceOnlineLogService.save(deviceOnlineLog);

        // 查询设备对应用户的信息
        if (Objects.isNull(deviceDetail.getUserId())) {
            return true;
        }
        if (status != 1) {
            // 设备下线通知
            log.info("user[{}] device[{}] is down...", deviceDetail.getUserId(), deviceDetail.getDeviceNo());

            Map<String, Object> param = new HashMap<>(7);
            param.put("deviceNo", deviceDetail.getDeviceNo());
            param.put("deviceName", deviceDetail.getName());
            param.put("publicIp", deviceDetail.getPublicIp());
            param.put("downTimeStr", DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            notifyBiz.notification(deviceDetail.getUserId(), NotifyBiz.NotifyType.DEVICE_DOWN, param);
        }

        return true;
    }

    @Resource
    private UserService userService;
    @Resource
    private NotifyBiz notifyBiz;

    @Override
    public int batchUpdateStatus(List<String> deviceNoList, int status) {
        if (deviceNoList.size() == 0) return 0;
        return this.baseMapper.batchUpdateStatus(deviceNoList, status);
    }

    @Override
    public int updateOutLineByTunnelId(Integer tunnelId) {
        return this.baseMapper.updateOutLineByTunnelId(tunnelId);
    }

    @Override
    public DeviceOnline getOneByDeviceNo(String deviceNo) {
        return this.baseMapper.selectOne(Wrappers.<DeviceOnline>lambdaQuery()
                .eq(DeviceOnline::getDeviceNo, deviceNo));
    }
}
