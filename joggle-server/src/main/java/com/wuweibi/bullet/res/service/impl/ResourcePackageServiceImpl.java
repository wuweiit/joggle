package com.wuweibi.bullet.res.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wuweibi.bullet.res.domain.*;
import com.wuweibi.bullet.res.entity.ResourcePackage;
import com.wuweibi.bullet.res.mapper.ResourcePackageMapper;
import com.wuweibi.bullet.res.service.ResourcePackageService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * (ResourcePackage)表服务实现类
 *
 * @author marker
 * @since 2022-10-30 15:48:47
 */
@Service
public class ResourcePackageServiceImpl extends ServiceImpl<ResourcePackageMapper, ResourcePackage> implements ResourcePackageService {


    @Override
    public Page<ResourcePackageVO> getPage(Page pageInfo, ResourcePackageAdminParam params) {

        LambdaQueryWrapper<ResourcePackage> qw = Wrappers.lambdaQuery();
        Page<ResourcePackage> page = this.baseMapper.selectPage(pageInfo, qw);

        Page<ResourcePackageVO> pageResult = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), pageInfo.getTotal());
        pageResult.setRecords(page.getRecords().stream().map(entity->{
            ResourcePackageVO vo = new ResourcePackageVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList()));
        return pageResult;
    }

    @Override
    public Page<ResourcePackageVO> getAdminList(Page pageInfo, ResourcePackageAdminParam params) {
        return this.baseMapper.selectAdminList(pageInfo, params);
    }

    @Override
    public Page<ResourcePackageListVO> getList(Page pageInfo, ResourcePackageParam params) {
        return  this.baseMapper.selectWebList(pageInfo, params);
    }

    @Override
    public ResourcePackage getByLevel(int level) {
        return this.baseMapper.selectOne(Wrappers.<ResourcePackage>lambdaQuery()
                .eq(ResourcePackage::getLevel, level)
                .eq(ResourcePackage::getStatus, 1));
    }

    @Override
    public List<PackageOptionVO> getOptionList() {
        List<ResourcePackage> list = this.list();
        return list.stream()
                .map(item -> {
                    return new PackageOptionVO(item.getId(), item.getName(), item.getName());
                })
                .collect(Collectors.toList());
    }
}
