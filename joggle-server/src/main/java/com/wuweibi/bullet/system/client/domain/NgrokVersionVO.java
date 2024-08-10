package com.wuweibi.bullet.system.client.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 *  程序版本vo
 * </p>
 *
 * @author marker
 * @since 2024-08-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class NgrokVersionVO implements Serializable {


    @ApiModelProperty("ngrok客户端版本 v1.3.9")
    private String clientVersion;


    @ApiModelProperty("ngrok服务端版本 v1.3.9")
    private String serverVersion;



}
