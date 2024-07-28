package com.wuweibi.bullet.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wuweibi.bullet.device.domain.dto.ServerTunnelAdminParam;
import com.wuweibi.bullet.device.domain.vo.ServerTunnelAdminVO;
import com.wuweibi.bullet.device.domain.vo.ServerTunnelNodeVO;
import com.wuweibi.bullet.device.domain.vo.TunnelOption;
import com.wuweibi.bullet.device.entity.ServerTunnel;
import com.wuweibi.bullet.device.mapper.ServerTunnelMapper;
import com.wuweibi.bullet.device.service.ServerTunnelService;
import com.wuweibi.bullet.utils.DateTimeUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 通道(ServerTunnel)表服务实现类
 *
 * @author makejava
 * @since 2022-04-28 21:27:37
 */
@Service
public class ServerTunnelServiceImpl extends ServiceImpl<ServerTunnelMapper, ServerTunnel> implements ServerTunnelService {

    @Override
    public List<TunnelOption> getOptionList() {
        return this.baseMapper.selectOptionList();
    }

    @Override
    public List<ServerTunnel> getListEnable() {
        return this.baseMapper.selectList(Wrappers.emptyWrapper());
    }

    @Override
    public boolean updateStatus(Integer tunnelId, int status, String version) {
        LambdaUpdateWrapper<ServerTunnel> wp = Wrappers.<ServerTunnel>lambdaUpdate()
                .eq(ServerTunnel::getId, tunnelId)
                .set(ServerTunnel::getStatus, status)
                .set(ServerTunnel::getServerUpTime, new Date());

        if (Objects.nonNull(version)) {
            wp.set(ServerTunnel::getVersion, version);
        }

        if (status == 0) {// 离线
            wp.set(ServerTunnel::getServerDownTime, new Date());
        }
        return this.update(wp);
    }

    @Override
    public Page<ServerTunnelAdminVO> getAdminPage(Page pageInfo, ServerTunnelAdminParam params) {
        long nowTime = new Date().getTime();
        Page<ServerTunnelAdminVO> page = this.baseMapper.selectAdminPage(pageInfo, params);
        page.getRecords().forEach(item->{
            item.setName("["+item.getCountry()+"|"+item.getArea()+"] "+item.getName());

            if(item.getServerUpTime() == null){
                item.setOnlineTime("-");
                return;
            }
            Date time = item.getServerUpTime().compareTo(item.getServerDownTime()) >=0?item.getServerDownTime(): item.getServerUpTime();
            if (time == null) {
                time = new Date();
            }
            // 如果在线则计算在线时间
            if (Objects.equals(1, item.getStatus())){
                String subTime =  DateTimeUtil.diffDate(time.getTime(), nowTime);
                item.setOnlineTime(subTime);
            }
        });
        return page;
    }

    @Override
    public Page<ServerTunnelNodeVO> getNodeStatusList(Page pageInfo) {
        return this.baseMapper.selectNodeStatusList(pageInfo);
    }

    @Override
    public boolean checkDomain(String baseDomain) {
        return this.baseMapper.checkDomain(baseDomain);
    }
}

