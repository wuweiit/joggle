package com.wuweibi.bullet.device.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 设备在线日志(DeviceOnlineLog)表实体类
 *
 * @author marker
 * @since 2023-01-23 19:46:35
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DeviceOnlineLog {
    
    /**
     * id
     */
    @TableId
    @ApiModelProperty("id")
  	private Long id;
    
    /**
     * userId
     */
    @ApiModelProperty("用户id")
 	private Long userId;

    /**
     * 设备id
     */
    @ApiModelProperty("设备id")
 	private Long deviceId;

    @ApiModelProperty("设备名称")
 	private String deviceName;
    
    /**
     * mac地址
     */
    @ApiModelProperty("mac地址")
 	private String macAddr;
    
    /**
     * 内网ip
     */
    @ApiModelProperty("内网ip")
 	private String intranetIp;
    
    /**
     * 公网ip
     */
    @ApiModelProperty("公网ip")
 	private String publicIp;
    
    /**
     * 通道id
     */
    @ApiModelProperty("通道id")
 	private Integer serverTunnelId;
    
    /**
     * 状态 1 上线 -1 下线
     */
    @ApiModelProperty("状态 1 上线 -1 下线")
 	private Integer status;

    @ApiModelProperty("操作系统")
    private String os;

    @ApiModelProperty("CPU架构")
    private String arch;
    
    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
 	private Date createTime;
    
    /**
     * 更新时间
     */
    @ApiModelProperty("更新时间")
 	private Date updateTime;

}
