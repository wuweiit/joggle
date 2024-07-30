package com.wuweibi.bullet.res.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuweibi.bullet.common.domain.IdDTO;
import com.wuweibi.bullet.common.domain.PageParam;
import com.wuweibi.bullet.entity.api.R;
import com.wuweibi.bullet.res.domain.PackageOptionVO;
import com.wuweibi.bullet.res.domain.ResourcePackageAdminParam;
import com.wuweibi.bullet.res.domain.ResourcePackageDTO;
import com.wuweibi.bullet.res.domain.ResourcePackageVO;
import com.wuweibi.bullet.res.entity.ResourcePackage;
import com.wuweibi.bullet.res.service.ResourcePackageService;
import com.wuweibi.bullet.res.service.UserPackageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * (ResourcePackage)表控制层
 *
 * @author marker
 * @since 2022-10-30 15:48:49
 */
@Slf4j
@RestController
@Api(value = "", tags = "套餐管理")
@RequestMapping("/admin/resource/package")
public class ResourcePackageAdminController {
    /**
     * 服务对象
     */
    @Resource
    private ResourcePackageService resourcePackageService;
    
    /**
     * 分页查询所有数据2
     *
     * @param page 分页对象
     * @param params 查询实体
     * @return 所有数据
     */
    @ApiOperation("套餐分页查询")
    @GetMapping("/list")
    public R<Page<ResourcePackageVO>> getPageList(PageParam page, ResourcePackageAdminParam params) {
        return R.ok(this.resourcePackageService.getAdminList(page.toMybatisPlusPage(), params));
    }

    @ApiOperation("套餐下拉选项")
    @GetMapping("/options")
    public R<List<PackageOptionVO>> getOptionList( ) {
        return R.ok(this.resourcePackageService.getOptionList());
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @ApiOperation("通过主键查询单条数据")
    @GetMapping("/detail")
    public R<ResourcePackage> detail(@RequestParam Serializable id) {
        return R.ok(this.resourcePackageService.getById(id));
    }

    /**
     * 新增数据
     * @param dto 实体对象
     * @return 新增结果
     */
    @ApiOperation("新增数据")
    @PostMapping
    public R<Boolean> save(@RequestBody @Valid ResourcePackageDTO dto) {
        ResourcePackage entity = new ResourcePackage();
        BeanUtils.copyProperties(dto, entity);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        return R.ok(this.resourcePackageService.save(entity));
    }

    /**
     * 修改数据
     * @param dto 实体对象
     * @return 修改结果
     */
    @ApiOperation("修改数据")
    @PutMapping
    public R<Boolean> update(@RequestBody @Valid ResourcePackageDTO dto) {
        if(dto.getId() == null){
            return R.fail("数据不存在");
        }

        ResourcePackage entity = this.resourcePackageService.getById(dto.getId());
        BeanUtils.copyProperties(dto, entity);
        entity.setUpdateTime(new Date());
        return R.ok(this.resourcePackageService.updateById(entity));
    }


    @Resource
    private UserPackageService userPackageService;

    /**
     * 删除套餐
     *
     * @param idDTO 主键
     * @return 删除结果
     */
    @ApiOperation("删除套餐")
    @DeleteMapping()
    public R<Boolean> deleteById(@RequestBody @Valid IdDTO idDTO) {
        Integer packageId = idDTO.getId();
        // 检查用户是开通过资源
        if (packageId == 1) {
            return R.fail("系统内置，不能删除");
        }
        if (userPackageService.checkPackageId(packageId)){
            return R.fail("已经有用户开通套餐，不能删除");
        }
        return R.ok(this.resourcePackageService.removeById(packageId));
    }

}
