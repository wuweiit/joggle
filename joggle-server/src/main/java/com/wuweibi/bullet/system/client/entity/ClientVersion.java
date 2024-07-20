package com.wuweibi.bullet.system.client.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 *
 * </p>
 *
 * @author marker
 * @since 2021-08-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("client_version")
public class ClientVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 类型 ：
     * CLIENT  客户端程序
     * SERVER  服务端程序
     */
    private String type;

    /**
     * 版本号
     */
    @TableField("version")
    private String version;

    /**
     * 标题
     */
    @TableField("title")
    private String title;


    private String os; // ":"darwin",
    
    private String arch; // ":"amd64",

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    /**
     * 下载URL地址
     */
    @TableField("download_url")
    private String downloadUrl;

    /**
     * 检查信息
     */
    @TableField("checksum")
    private String checksum;

    /**
     * 状态 1上架 0下架
     */
    @TableField("status")
    private Boolean status;


}
