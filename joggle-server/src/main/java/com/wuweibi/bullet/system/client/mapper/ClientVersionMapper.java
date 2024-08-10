package com.wuweibi.bullet.system.client.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuweibi.bullet.system.client.domain.ClientVersionAdminListVO;
import com.wuweibi.bullet.system.client.domain.NgrokVersionVO;
import com.wuweibi.bullet.system.client.entity.ClientVersion;
import com.wuweibi.bullet.system.domain.dto.ClientVersionParam;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author marker
 * @since 2021-08-12
 */
public interface ClientVersionMapper extends BaseMapper<ClientVersion> {

    Page<ClientVersionAdminListVO> selectAdminList(Page pageInfo, @Param("params") ClientVersionParam params);

    /**
     * 获取最大版本号
     * @return
     */
    NgrokVersionVO selectMaxVersion();

}
