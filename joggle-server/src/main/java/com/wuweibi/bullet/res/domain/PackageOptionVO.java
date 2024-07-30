package com.wuweibi.bullet.res.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 套餐下拉对象
 *
 * @author marker
 * @since 2022-10-30 15:48:50
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
public class PackageOptionVO {

    /**
     * id
     */
    @ApiModelProperty("id")
  	private Integer id;    

    /**
     * 资源包名称
     */        
    @ApiModelProperty("资源包名称")
 	private String name;
    /**
     * 资源包名称(标签)
     */
    @ApiModelProperty("资源包名称(标签)")
 	private String label;

}
