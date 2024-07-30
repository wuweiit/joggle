package com.wuweibi.bullet.system.domain.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 *    用户模型
 * </p>
 *
 * @author marker
 * @since 2017-12-08
 */
@Data
public class UserAdminDetailVO {

    private static final long serialVersionUID = 1L;

	private Long id;
	private String email;
	private String nickname;
	private String username;
	private String password;
	private String agree;

	/**
	 * 头像
	 */
	@TableField("icon")
	private String icon;

	/**
	 * 登录时间
	 */
	@ApiModelProperty("注册时间")
	private Date loginTime;

	@ApiModelProperty("注册时间")
	private Date createdTime;

	/**
	 * 余额
	 */
	@TableField("balance")
	private BigDecimal balance;

	/**
	 * 是否启用
	 */
	@TableField("enabled")
	private Integer enabled;


	/**
	 * 激活码/邀请码
	 */
	@TableField("activate_code")
	private String activateCode;


	@ApiModelProperty("用户认证状态")
	private Integer userCertification;


	@ApiModelProperty("用户套餐名称")
	private String userPackageName;






}
