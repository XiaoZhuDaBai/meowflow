package com.meowflow.infra.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 调用日志 Mapper
 */
@Mapper
public interface AIInvokeLogMapper extends BaseMapper<AIInvokeLog> {

    /**
     * 按模型分组统计成本
     */
    @Select("""
            SELECT model, provider,
                   COUNT(*) as invoke_count,
                   COALESCE(SUM(cost_amount), 0) as total_cost,
                   COALESCE(SUM(prompt_tokens + completion_tokens), 0) as total_tokens
            FROM mf_ai_invoke_log
            WHERE status = 1
              AND (#{userId} IS NULL OR user_id = #{userId})
              AND (#{from} IS NULL OR create_time >= #{from})
              AND (#{to} IS NULL OR create_time <= #{to})
            GROUP BY model, provider
            ORDER BY total_cost DESC
            """)
    List<ModelCostRow> selectCostByModel(@Param("userId") Long userId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    interface ModelCostRow {
        String getModel();
        String getProvider();
        int getInvokeCount();
        BigDecimal getTotalCost();
        long getTotalTokens();
    }
}
