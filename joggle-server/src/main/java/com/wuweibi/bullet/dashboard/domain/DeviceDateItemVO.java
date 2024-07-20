package com.wuweibi.bullet.dashboard.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeviceDateItemVO {

    @ApiModelProperty("时间 yyyy-MM-dd")
    private String time;

    @ApiModelProperty("全部流量MB")
    private BigDecimal flow = BigDecimal.ZERO;

    @ApiModelProperty("入网流量MB")
    private BigDecimal flowIn = BigDecimal.ZERO;

    @ApiModelProperty("出网流量MB")
    private BigDecimal flowOut = BigDecimal.ZERO;

    @ApiModelProperty("链接数量")
    private BigDecimal link = BigDecimal.ZERO;

}
