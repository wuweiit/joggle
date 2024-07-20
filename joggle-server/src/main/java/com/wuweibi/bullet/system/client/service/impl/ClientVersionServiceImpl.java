package com.wuweibi.bullet.system.client.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wuweibi.bullet.config.properties.AliOssProperties;
import com.wuweibi.bullet.domain.dto.ClientInfoDTO;
import com.wuweibi.bullet.system.client.domain.ClientVersionAdminListVO;
import com.wuweibi.bullet.system.client.entity.ClientVersion;
import com.wuweibi.bullet.system.client.mapper.ClientVersionMapper;
import com.wuweibi.bullet.system.client.service.ClientVersionService;
import com.wuweibi.bullet.system.domain.dto.ClientVersionParam;
import com.wuweibi.bullet.utils.SpringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author marker
 * @since 2021-08-12
 */
@Service
public class ClientVersionServiceImpl extends ServiceImpl<ClientVersionMapper, ClientVersion> implements ClientVersionService {

    @Override
    public ClientVersion getNewVersion(ClientInfoDTO clientInfoDTO) {
        return this.baseMapper.selectOne(Wrappers.<ClientVersion>lambdaQuery()
                .eq(ClientVersion::getType, clientInfoDTO.getApp_id())
                .eq(ClientVersion::getStatus, 1)
                .eq(ClientVersion::getOs, clientInfoDTO.getOs())
                .eq(ClientVersion::getArch, clientInfoDTO.getArch())
                .orderByDesc(ClientVersion::getCreateTime)
                .last("limit 1")
        );
    }

    @Resource
    private AliOssProperties aliOssProperties;

    @Override
    public int updateChecksumByOsArch(String version, String os, String arch, String binFilePath, String checksum) {
        String type = "CLIENT";
        if (binFilePath.contains("ngrokd")){
            type = "SERVER";
        }

        ClientVersion clientVersion = this.baseMapper.selectOne(Wrappers.<ClientVersion>lambdaQuery()
                .eq(ClientVersion::getOs, os)
                .eq(ClientVersion::getArch, arch)
                .eq(ClientVersion::getType, type)
                .eq(ClientVersion::getStatus, 1)
        );
        if (clientVersion == null) return 0;

        // 生产环境才做URL更新
        String downloadURL = String.format("%s/client/%s/%s", aliOssProperties.getPublicServerUrl(), version, binFilePath);
        if (!SpringUtils.isProduction()) {
            downloadURL = String.format("%s/client/%s/%s", "http://192.168.1.6:80", version, binFilePath);
        }
        clientVersion.setDownloadUrl(downloadURL);
        clientVersion.setChecksum(checksum);
        clientVersion.setTitle(String.format("joggle-%s-%s",type.toLowerCase(), version));
        clientVersion.setVersion(version);
        clientVersion.setStatus(true);
        clientVersion.setUpdateTime(new Date());
        return this.baseMapper.updateById(clientVersion);
    }

    @Override
    public String getMaxVersion() {
        ClientVersion clientVersion = this.getById(1);
        if(clientVersion == null){
            return "v1.3.0";
        }
        return String.format("v%s", clientVersion.getVersion());
    }

    @Override
    public Page<ClientVersionAdminListVO> getAdminList(Page pageInfo, ClientVersionParam params) {
        return this.baseMapper.selectAdminList(pageInfo, params);
    }
}
