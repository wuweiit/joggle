package com.wuweibi.bullet.orders.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
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
public class Orders extends Model<Orders> {
    //订单id
    @TableId(type = IdType.AUTO)
    private Integer id;
    //订单号
    private String orderNo;
    //用户id
    private Long userId;
    //资源类型 1端口 2域名 3流量 4 充值
    private Integer resourceType;
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
    @ApiModelProperty("退款金额")
    private BigDecimal refundMoney;
    @ApiModelProperty("退款数量")
    private Long refundAmount;
    //支付方式 1余额 2支付宝
    private Integer payType;
    //订单状态 0待支付 1已支付 2 取消 3退款中 4已退款
    private Integer status;
    //三方交易号
    private String tradeNo;
    //支付时间
    private Date payTime;
    //退款时间
    private Date refundTime;
    //取消时间
    private Date cancelTime;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
    // 用户下单ip
    private String userIp;

}

