package com.wuweibi.bullet.device.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ServerTunnelAdminVO {

    private Integer id;
    //通道名称
    private String name;
    private String country;
    private String area;
    //宽带 MB
    private Integer broadband;
    //线路通道地址
    private String serverAddr;

    @ApiModelProperty("通道版本号")
    private String version;

    @ApiModelProperty("通道上线时间")
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


    @ApiModelProperty("在线时长")
    private String onlineTime;

    @ApiModelProperty("服务器到期时间")
    private Date serverEndTime;

    @ApiModelProperty("通道服务器上线时间")
    private Date serverUpTime;

    @ApiModelProperty("通道服务器离线时间")
    private Date serverDownTime;




}
