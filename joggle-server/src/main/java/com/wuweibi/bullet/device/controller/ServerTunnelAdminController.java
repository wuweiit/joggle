package com.wuweibi.bullet.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuweibi.bullet.annotation.JwtUser;
import com.wuweibi.bullet.common.domain.IdDTO;
import com.wuweibi.bullet.common.domain.PageParam;
import com.wuweibi.bullet.conn.WebsocketPool;
import com.wuweibi.bullet.device.domain.dto.ServerTunnelAdminDTO;
import com.wuweibi.bullet.device.domain.dto.ServerTunnelAdminParam;
import com.wuweibi.bullet.device.domain.dto.TunnelCheckUpdateDTO;
import com.wuweibi.bullet.device.domain.vo.ServerTunnelAdminVO;
import com.wuweibi.bullet.device.entity.ServerTunnel;
import com.wuweibi.bullet.device.service.ServerTunnelService;
import com.wuweibi.bullet.domain.domain.session.Session;
import com.wuweibi.bullet.entity.api.R;
import com.wuweibi.bullet.protocol.MsgCheckUpdate;
import com.wuweibi.bullet.service.DomainService;
import com.wuweibi.bullet.websocket.Bullet3Annotation;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Date;

/**
 * 通道(TServerTunnel)表控制层
 *
 * @author marker
 * @since 2022-09-16 20:45:17
 */
@RestController
@RequestMapping("/admin/server/tunnel")
public class ServerTunnelAdminController {
    /**
     * 服务对象
     */
    @Resource
    private ServerTunnelService serverTunnelService;

    @Resource
    private DomainService domainService;


    /**
     * 通道分页查询
     *
     * @param page   分页对象
     * @param params 查询实体
     * @return 所有数据
     */
    @ApiOperation("通道分页查询")
    @GetMapping("/list")
    public R<Page<ServerTunnelAdminVO>> getPageList(PageParam page, ServerTunnelAdminParam params) {
        return R.ok(this.serverTunnelService.getAdminPage(page.toMybatisPlusPage(), params));
    }

    /**
     * 通道详情
     *
     * @return
     */
    @ApiOperation("通道详情")
    @GetMapping(value = "/detail")
    public R<ServerTunnel> detail(IdDTO dto) {
        ServerTunnel serverTunnel = serverTunnelService.getById(dto.getId());
        return R.ok(serverTunnel);
    }

    /**
     * 新建通道
     *
     * @return
     */
    @ApiOperation("新建通道")
    @PostMapping(value = "")
    public R create(@RequestBody @Valid ServerTunnelAdminDTO dto) {
        ServerTunnel serverTunnel = new ServerTunnel();
        BeanUtils.copyProperties(dto, serverTunnel);
        serverTunnel.setCreateTime(new Date());
        serverTunnel.setStatus(0);

        serverTunnelService.save(serverTunnel);
        return R.ok();
    }


    /**
     * 新建通道
     *
     * @return
     */
    @ApiOperation("新建通道")
    @PutMapping(value = "")
    public R update(@RequestBody @Valid ServerTunnelAdminDTO dto) {
        if (dto.getId() == null) {
            return R.fail("id不能为空");
        }
        ServerTunnel serverTunnel = serverTunnelService.getById(dto.getId());
        BeanUtils.copyProperties(dto, serverTunnel);

        serverTunnelService.updateById(serverTunnel);
        return R.ok();
    }


    /**
     * 删除通道
     * @return
     */
    @ApiOperation("删除通道")
    @DeleteMapping(value = "")
    public R delete(@RequestBody @Valid IdDTO dto) {
        Integer id = dto.getId();
        ServerTunnel serverTunnel = serverTunnelService.getById(id);
        if (serverTunnel == null) {
            return R.fail("通道不存在");
        }

        // 校验通道资源
        boolean isUse = domainService.checkServerTunnelUse(serverTunnel.getId());
        if (isUse) {
            return R.fail("通道存在资源，不能删除");
        }

        serverTunnelService.removeById(serverTunnel.getId());
        return R.ok();
    }


    @Resource
    private WebsocketPool websocketPool;


    /**
     * 检查更新接口
     *
     * @return
     */
    @ApiOperation("触发通道检查更新")
    @PostMapping("/trigger-update")
    public R<Boolean> triggerUpdate(@JwtUser Session session,
                                  @RequestBody @Valid TunnelCheckUpdateDTO dto) {
        Long userId = session.getUserId();
        ServerTunnel serverTunnel = serverTunnelService.getById(dto.getTunnelId());
        if (serverTunnel == null){
            return R.fail("通道不存在");
        }

        // 发送切换消息给设备
        Bullet3Annotation annotation = websocketPool.getByTunnelId(dto.getTunnelId());
        if (annotation == null) {
            return R.fail("通道不在线");
        }
        MsgCheckUpdate msg = new MsgCheckUpdate();
        annotation.sendMessageToServer(msg);
        return R.ok();
    }

}
