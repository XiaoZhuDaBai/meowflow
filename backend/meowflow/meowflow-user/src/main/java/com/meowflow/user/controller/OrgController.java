package com.meowflow.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.result.Result;
import com.meowflow.user.dto.OrgTree;
import com.meowflow.user.entity.Org;
import com.meowflow.user.service.OrgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "组织管理", description = "组织架构CRUD、岗位管理等接口")
@RestController
@RequestMapping("/api/v1/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    @Operation(summary = "创建组织", description = "创建新组织")
    @PostMapping
    public Result<OrgTree> create(@RequestBody Org org) {
        OrgTree tree = orgService.createOrg(org);
        return Result.success(tree);
    }

    @Operation(summary = "更新组织", description = "更新已有组织信息")
    @PutMapping("/{id}")
    public Result<OrgTree> update(
            @Parameter(description = "组织ID") @PathVariable Long id,
            @RequestBody Org org) {
        OrgTree tree = orgService.updateOrg(id, org);
        return Result.success(tree);
    }

    @Operation(summary = "删除组织", description = "删除指定组织")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        orgService.deleteOrg(id);
        return Result.success();
    }

    @Operation(summary = "获取组织详情", description = "根据ID获取组织详细信息")
    @GetMapping("/{id}")
    public Result<OrgTree> getById(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        OrgTree tree = orgService.getOrgById(id);
        return Result.success(tree);
    }

    @Operation(summary = "获取组织树", description = "获取完整的组织架构树")
    @GetMapping("/tree")
    public Result<List<OrgTree>> getOrgTree() {
        List<OrgTree> tree = orgService.getOrgTree();
        return Result.success(tree);
    }

    @Operation(summary = "获取子组织", description = "获取指定组织的直接子组织")
    @GetMapping("/{id}/children")
    public Result<List<Org>> getChildren(
            @Parameter(description = "组织ID") @PathVariable Long id) {
        List<Org> children = orgService.getChildren(id);
        return Result.success(children);
    }

    @Operation(summary = "分页查询组织", description = "支持关键词、状态筛选")
    @GetMapping
    public Result<IPage<OrgTree>> pageQuery(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Long pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Long pageSize) {
        IPage<OrgTree> page = orgService.pageQuery(keyword, status, pageNum, pageSize);
        return Result.success(page);
    }
}
