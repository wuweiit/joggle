package com.wuweibi.bullet.device.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 设备在线日志(DeviceOnlineLog)分页对象
 *
 * @author marker
 * @since 2023-01-23 19:46:35
 */
@SuppressWarnings("serial")
@Data
public class DeviceOnlineLogVO {

    /**
     * id
     */
    @ApiModelProperty("id")
  	private Long id;    

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
     * 状态 1 上线 0 下线
     */        
    @ApiModelProperty("状态 1 上线 0 下线")
 	private Integer status;

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
