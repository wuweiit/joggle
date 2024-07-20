package com.wuweibi.bullet.device.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class TunnelCheckUpdateDTO {


    @ApiModelProperty("通道id")
    @NotNull(message = "{com.wuweibi.bullet.device.id.NotNull}")
    private Integer tunnelId;



}
