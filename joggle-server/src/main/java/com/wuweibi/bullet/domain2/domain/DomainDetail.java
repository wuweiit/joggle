package com.wuweibi.bullet.domain2.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 *  	域名与端口
 * </p>
 *
 * @author marker
 * @since 2017-12-09
 */
@Data
public class DomainDetail   {

    private static final long serialVersionUID = 1L;

	private Long id;

	/**
	 * 通道id
	 */
	private Integer serverTunnelId;

	/**
	 * 二级域名前缀或端口
	 */
	private String domain;
	private String domainFull;

	/**
	 *  类型： 1 端口 2 域名
	 */
	private Integer type;

	/**
	 * 销售价格（元/月）
	 */
	@TableField(value = "sales_price")
	private BigDecimal salesPrice;
	/**
	 * 销售价格（元/月）
	 */
	@TableField(value = "original_price")
	private BigDecimal originalPrice;


	/**
	 * 发售时间
	 */
    @TableField(value = "create_time")
	private Date createTime;

	/**
	 * 购买时间
	 */
	@TableField(value = "buy_time")
	private Date buyTime;
	/**
	 * 到期时间
	 */
	@TableField(value = "due_time")
	private Date dueTime;

	/**
	 * 所有人ID
	 */
	@TableField(value = "user_id")
	private Long userId;

	/**
	 * 状态：1已售、0释放、-1 禁售
	 */
	@TableField(value = "status")
	private Integer status;




	/**
	 * 获取TypeName
	 * @return
	 */
	public String getTypeName(){
    	switch (this.type){
			case 1: return "端口";
			case 2: return "域名";
		}
		return "-";
	}

}