# 同步：FE builtinTemplates.ts → BE BuiltinTemplateCatalog.java

`regen-builtin-catalog.js` 读 `frontend/meowflow-ui/src/mock/builtinTemplates.ts`，
把它声明的 17 个官方模板（含节点 id、坐标、config、边类型 condition/loop/error/default）
转换为 Java 17 的 `backend/meowflow/meowflow-template/src/main/java/com/meowflow/template/catalog/BuiltinTemplateCatalog.java`。

## 使用

```bash
node scripts/regen-builtin-catalog.js
```

## 何时需要运行

- 新增/删除/重命名任何官方模板时
- 修改任何 `n(...)` 或 `e(...)` 调用时（节点 / 边参数）
- 修改 `entry(...)` 元数据（name / desc / 标签 / 使用次数 / 评分）

## 注意事项

- 运行前先确保 TS 文件能正常 import / 类型检查通过。
- 脚本是幂等的：连续运行不会重复写入内容。
- 生成后的 Java 文件保留了 `add(...)` `node(...)` `edge(...)` `edgeCondition(...)` `edgeLoop(...)` `edgeError(...)` 这些辅助方法的原始签名。
- 输出文件统计每个模板的节点/边数，便于发现遗漏。