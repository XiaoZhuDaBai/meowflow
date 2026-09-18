package com.meowflow.template.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.common.util.IdGeneratorFactory;
import com.meowflow.template.dto.TemplateReviewRequest;
import com.meowflow.template.entity.Template;
import com.meowflow.template.entity.TemplateReview;
import com.meowflow.template.repository.TemplateRepository;
import com.meowflow.template.repository.TemplateReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateReviewService {

    private final TemplateReviewRepository reviewRepository;
    private final TemplateRepository templateRepository;
    private final IdGeneratorFactory idGenerator;

    @Transactional(rollbackFor = Exception.class)
    public TemplateReview submitForReview(Long templateId) {
        Template template = templateRepository.findById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }

        if (!"pending".equals(template.getReviewStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "模板当前状态不允许提交审核");
        }

        TemplateReview review = new TemplateReview();
        review.setId(idGenerator.nextId());
        review.setTemplateId(templateId);
        review.setUserId(getCurrentUserId());
        review.setAction("submit");
        review.setResult("pending");
        review.setCreateBy(getCurrentUserId());

        reviewRepository.insert(review);
        log.info("Template {} submitted for review by user {}", templateId, getCurrentUserId());

        return review;
    }

    @Transactional(rollbackFor = Exception.class)
    public TemplateReview approveTemplate(Long templateId, String comment) {
        Template template = templateRepository.findById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }
        if (!"pending".equals(template.getReviewStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "模板当前状态不允许审核");
        }
        return doReview(template, templateId, "approve", comment, "approved");
    }

    @Transactional(rollbackFor = Exception.class)
    public TemplateReview rejectTemplate(Long templateId, String comment) {
        Template template = templateRepository.findById(templateId);
        if (template == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "模板不存在");
        }
        if (!"pending".equals(template.getReviewStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR, "模板当前状态不允许审核");
        }
        return doReview(template, templateId, "reject", comment, "rejected");
    }

    private TemplateReview doReview(Template template, Long templateId, String action, String comment, String result) {
        template.setReviewStatus(result);
        template.setReviewComment(comment);
        template.setReviewBy(getCurrentUserIdLong());
        template.setReviewTime(LocalDateTime.now());
        template.setUpdateBy(getCurrentUserIdLong());
        templateRepository.updateById(template);

        TemplateReview review = new TemplateReview();
        review.setId(idGenerator.nextId());
        review.setTemplateId(templateId);
        review.setUserId(template.getCreateBy());
        review.setReviewerId(getCurrentUserId());
        review.setAction(action);
        review.setComment(comment);
        review.setResult(result);
        review.setCreateBy(getCurrentUserId());

        reviewRepository.insert(review);
        log.info("Template {} action={} by reviewer {}", templateId, action, getCurrentUserId());
        return review;
    }

    public IPage<TemplateReview> getPendingReviews(Integer pageNum, Integer pageSize) {
        Page<TemplateReview> page = new Page<>(pageNum, pageSize);
        return reviewRepository.findByResult(page, "pending");
    }

    public IPage<TemplateReview> getMyReviewHistory(Integer pageNum, Integer pageSize) {
        Page<TemplateReview> page = new Page<>(pageNum, pageSize);
        return reviewRepository.findByReviewerId(page, getCurrentUserId());
    }

    private Long getCurrentUserId() {
        UserContext ctx = UserContextHolder.get();
        return ctx != null ? ctx.getUserId() : 0L;
    }

    private Long getCurrentUserIdLong() {
        UserContext ctx = UserContextHolder.get();
        return ctx != null ? ctx.getUserId() : 0L;
    }
}


