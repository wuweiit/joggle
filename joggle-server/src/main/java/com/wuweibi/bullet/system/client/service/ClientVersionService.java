package com.wuweibi.bullet.system.client.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wuweibi.bullet.domain.dto.ClientInfoDTO;
import com.wuweibi.bullet.system.client.domain.ClientVersionAdminListVO;
import com.wuweibi.bullet.system.client.domain.NgrokVersionVO;
import com.wuweibi.bullet.system.client.entity.ClientVersion;
import com.wuweibi.bullet.system.domain.dto.ClientVersionParam;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author marker
 * @since 2021-08-12
 */
public interface ClientVersionService extends IService<ClientVersion> {

    /**
     * 获取新版本
     * @return
     * @param clientInfoDTO
     */
    ClientVersion getNewVersion(ClientInfoDTO clientInfoDTO);

    /**
     * 更新checksum
     *
     * @param version
     * @param os
     * @param arch
     * @param checksum
     * @param binFilePath 二进制文件路径
     * @return
     */
    int updateChecksumByOsArch(String version, String os, String arch, String binFilePath, String checksum);

    /**
     * 获取最大版本
     * @return
     */
    NgrokVersionVO getMaxVersion();

    /**
     * 分页查询客户端
     * @param pageInfo 分页参数
     * @param params 条件参数
     * @return
     */
    Page<ClientVersionAdminListVO> getAdminList(Page pageInfo, ClientVersionParam params);
}
