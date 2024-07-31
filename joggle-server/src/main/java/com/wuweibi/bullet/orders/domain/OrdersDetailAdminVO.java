package com.wuweibi.bullet.orders.domain;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * (Orders)表实体类
 *
 * @author makejava
 * @since 2022-07-21 16:16:09
 */
@Data
@SuppressWarnings("serial")
public class OrdersDetailAdminVO {
    //订单id
    private Integer id;

    //订单号
    private String orderNo;

    //资源类型 1域名 2端口 3流量 4 充值
    @ApiModelProperty("资源类型")
    private Integer resourceType;

    @ApiModelProperty("资源类型")
    private String resourceTypeName;
    //资源id
    private Long domainId;
    //购买量 单位：秒、MB
    private Long amount;
    //资源名称
    private String name;
    //原价
    private BigDecimal priceAmount;
    //优惠金额
    private BigDecimal discountAmount;
    //支付价格
    private BigDecimal payAmount;
    //支付方式 1余额 2支付宝
    private Integer payType;
    private String payTypeName;

    @ApiModelProperty("订单状态 0待支付 1已支付 2 取消 3退款中 4已退款")
    private Integer status;

    @ApiModelProperty("订单状态名称")
    private String statusName;

    //
    @ApiModelProperty("三方交易号")
    private String tradeNo;
    //
    @ApiModelProperty("支付时间")
    private Date payTime;
    //
    @ApiModelProperty("退款时间")
    private Date refundTime;
    //
    @ApiModelProperty("取消时间")
    private Date cancelTime;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("更新时间")
    private Date updateTime;

    /**
     * 购买用户昵称
     */
    @ApiModelProperty("购买用户昵称")
    private String buyerNickname;

    @ApiModelProperty("购买用户Email")
    private String buyerEmail;

    @ApiModelProperty("用户下单ip")
    private String userIP;

}

