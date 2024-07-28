package com.wuweibi.bullet.device.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 通道(ServerTunnel)表实体类
 *
 * @author makejava
 * @since 2022-04-28 21:27:32
 */
@SuppressWarnings("serial")
@Data
@TableName("t_server_tunnel")
public class ServerTunnel  {

    @TableId
    private Integer id;

    // 用户id 公用 为 0
    private Long userId;
    //通道名称
    private String name;
    private String country;
    private String area;
    //宽带 MB
    private Integer broadband;
    //线路通道地址
    private String serverAddr;
    //上线时间
    private Date createTime;

    // 价格类型 1免费 2包月 3包年
    private Integer priceType;

    // 销售价格（元/周期）
    private BigDecimal salesPrice;

    // 原价（元/周期）
    private BigDecimal originalPrice;

    // 是否可购买 1可 0不可
    private Integer buyStatus;

    @ApiModelProperty("在线状态 1在线 0不在线")
    private Integer status;

    @ApiModelProperty("服务器到期时间")
    private Date serverEndTime;

    @ApiModelProperty("服务器Token")
    private String token;

    @ApiModelProperty("启用扣量 1是 0 否")
    private Integer enableFlow;

    @ApiModelProperty("启用TLS 1是 0否")
    private Integer enableTls;

    @ApiModelProperty("服务器上线时间")
    private Date serverUpTime;

    @ApiModelProperty("服务器离线时间")
    private Date serverDownTime;

    @ApiModelProperty("通道版本")
    private String version;
}

