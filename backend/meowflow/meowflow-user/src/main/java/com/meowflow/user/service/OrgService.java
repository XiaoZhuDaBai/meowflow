package com.meowflow.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.user.dto.OrgTree;
import com.meowflow.user.entity.Org;
import com.meowflow.user.repository.OrgRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgRepository orgRepository;

    @Transactional
    public OrgTree createOrg(Org org) {
        if (org.getParentId() != null && org.getParentId() != 0L) {
            Org parent = orgRepository.findById(org.getParentId())
                    .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "父组织不存在"));
        } else {
            org.setParentId(0L);
        }

        org.setStatus(StringUtils.hasText(org.getStatus()) ? org.getStatus() : "active");
        org.setCreateBy(currentUserId());
        org.setCreateTime(LocalDateTime.now());

        orgRepository.insert(org);

        return convertToOrgTree(org);
    }

    @Transactional
    public OrgTree updateOrg(Long orgId, Org request) {
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "组织不存在"));

        if (request.getParentId() != null && !request.getParentId().equals(org.getParentId())) {
            Org newParent = orgRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "父组织不存在"));

            if (request.getParentId().equals(orgId)) {
                throw new BizException(ResultCode.BIZ_ERROR, "不能将自己设为父组织");
            }

            org.setParentId(request.getParentId());
        }

        if (StringUtils.hasText(request.getName())) {
            org.setName(request.getName());
        }
        if (StringUtils.hasText(request.getCode())) {
            org.setCode(request.getCode());
        }
        if (StringUtils.hasText(request.getLeader())) {
            org.setLeader(request.getLeader());
        }
        if (StringUtils.hasText(request.getPhone())) {
            org.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            org.setEmail(request.getEmail());
        }
        if (request.getSort() != null) {
            org.setSort(request.getSort());
        }
        if (StringUtils.hasText(request.getStatus())) {
            org.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getRemark())) {
            org.setRemark(request.getRemark());
        }

        org.setUpdateBy(currentUserId());
        org.setUpdateTime(LocalDateTime.now());

        orgRepository.updateById(org);

        return convertToOrgTree(org);
    }

    @Transactional
    public void deleteOrg(Long orgId) {
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "组织不存在"));

        int childCount = orgRepository.countByParentId(orgId);
        if (childCount > 0) {
            throw new BizException(ResultCode.BIZ_ERROR, "请先删除子组织");
        }

        orgRepository.deleteById(orgId);
    }

    public OrgTree getOrgById(Long orgId) {
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "组织不存在"));
        return convertToOrgTree(org);
    }

    public List<OrgTree> getOrgTree() {
        List<Org> allOrgs = orgRepository.selectList(null);
        return buildOrgTree(allOrgs, 0L);
    }

    public IPage<OrgTree> pageQuery(String keyword, String status, Long pageNum, Long pageSize) {
        Page<Org> page = new Page<>(pageNum, pageSize);
        IPage<Org> result = orgRepository.pageQuery(page, keyword, status);
        return result.convert(this::convertToOrgTree);
    }

    public List<Org> getChildren(Long orgId) {
        return orgRepository.findByParentId(orgId);
    }

    private List<OrgTree> buildOrgTree(List<Org> orgs, Long parentId) {
        return orgs.stream()
                .filter(org -> {
                    Long pid = org.getParentId() != null ? org.getParentId() : 0L;
                    return pid.equals(parentId);
                })
                .sorted(Comparator.comparing(Org::getSort, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(org -> {
                    OrgTree tree = convertToOrgTree(org);
                    tree.setChildren(buildOrgTree(orgs, org.getId()));
                    return tree;
                })
                .collect(Collectors.toList());
    }

    private OrgTree convertToOrgTree(Org org) {
        return OrgTree.builder()
                .id(org.getId())
                .parentId(org.getParentId())
                .name(org.getName())
                .code(org.getCode())
                .leader(org.getLeader())
                .phone(org.getPhone())
                .email(org.getEmail())
                .sort(org.getSort())
                .status(org.getStatus())
                .build();
    }

    private static Long currentUserId() {
        return UserContextHolder.getUserId();
    }
}
