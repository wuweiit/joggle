package com.wuweibi.bullet.system.domain.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class UserAdminParam {


    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("启用状态 1正常 0禁用")
    private Integer enabled;

    @ApiModelProperty("套餐id")
    private Integer packageId;

}
