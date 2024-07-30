package com.wuweibi.bullet.res.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wuweibi.bullet.res.domain.*;
import com.wuweibi.bullet.res.entity.ResourcePackage;

import java.util.List;

/**
 * (ResourcePackage)表服务接口
 *
 * @author marker
 * @since 2022-10-30 15:48:47
 */
public interface ResourcePackageService extends IService<ResourcePackage> {


    /**
     * 分页查询数据
     * @param pageInfo 分页对象
     * @param params 参数
     * @return
     */
    Page<ResourcePackageVO> getPage(Page pageInfo, ResourcePackageAdminParam params);



    Page<ResourcePackageVO> getAdminList(Page pageInfo, ResourcePackageAdminParam params);

    Page<ResourcePackageListVO>  getList(Page toMybatisPlusPage, ResourcePackageParam params);

    ResourcePackage getByLevel(int level);

    /**
     * 获取套餐下拉选项
     * @return
     */
    List<PackageOptionVO> getOptionList();

}
