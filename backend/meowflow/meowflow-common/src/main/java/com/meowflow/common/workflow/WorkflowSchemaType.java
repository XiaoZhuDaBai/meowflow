package com.meowflow.common.workflow;

/**
 * 工作流定义 Schema 版本标识（与 mf_wf_workflow_version.definition 字段配套）。
 *
 * <p>喵流目前在工作流市场上流通三套"工作流 JSON"，它们并非完全等价：
 * <ul>
 *   <li><b>Full Definition</b>（{@link WorkflowFullDefinition}）：最完整的引擎侧 JSON，
 *       包含节点 + 连线 + 输入输出 Schema + 全局变量，用于：
 *       <ul>
 *         <li>执行器直接加载（{@code wf-executor} / {@code WorkflowExecutor}）</li>
 *         <li>版本发布与回滚（{@link com.meowflow.workflow.entity.WorkflowVersion}）</li>
 *       </ul></li>
 *   <li><b>Editor WorkflowJson</b>（{@link EditorWorkflowJson}）：前端 ReactFlow 编辑器使用，
 *       节点带有 id/type/position/data，连线带有 source/target/sourceHandle/targetHandle/edgeType。</li>
 *   <li><b>Market WorkflowGraph</b>（{@link MarketWorkflowGraph}）：模板市场 / 详情页预览使用，
 *       仅保存归一化的节点矩形（cx, cy, w, h）与连线连点，用于 SVG 渲染。</li>
 * </ul>
 *
 * <p>三套之间可以无损互转，由 {@link WorkflowJsonConverter} 完成。
 * <p>类型为枚举而不是字符串常量集合，便于在大对象中作为 {@code schemaType} 字段值参与序列化。
 *
 * @author MeowFlow Team
 */
public enum WorkflowSchemaType {
    FULL,
    EDITOR,
    MARKET
}
