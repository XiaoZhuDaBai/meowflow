package com.meowflow.template.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 内置模板节点构建工具。
 * <p>
 * 用于以声明方式生成带 x/y 坐标的工作流节点 JSON。
 */
public final class BuiltinNode {

    private BuiltinNode() {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Node {
        private String id;
        private String type;
        private String name;
        private Double x;
        private Double y;
        private String category;
        private String description;
        private Map<String, Object> data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Edge {
        private String id;
        private String source;
        private String target;
        private String label;
        private String type;
        private Map<String, Object> data;
    }

    /** 创建一个带初始 data 的节点 */
    public static Node of(String id, String type, String name, double x, double y) {
        Node n = new Node();
        n.setId(id);
        n.setType(type);
        n.setName(name);
        n.setX(x);
        n.setY(y);
        n.setData(new HashMap<>());
        return n;
    }

    public static Node of(String id, String type, String name, double x, double y, String category) {
        Node n = of(id, type, name, x, y);
        n.setCategory(category);
        return n;
    }

    /** 创建一条默认连线（source -> target） */
    public static Edge edge(String id, String source, String target) {
        return new Edge(id, source, target, null, "default", null);
    }

    /** 创建带标签的连线 */
    public static Edge edge(String id, String source, String target, String label) {
        return new Edge(id, source, target, label, null, null);
    }

    /**
     * 链式生成一列水平排列的节点（从 xStart 开始，水平间距 dx，垂直保持同一行）。
     */
    public static Node[] chain(String prefix, String[] types, String[] names, double y, double xStart, double dx) {
        Node[] nodes = new Node[types.length];
        for (int i = 0; i < types.length; i++) {
            nodes[i] = of(prefix + "-" + (i + 1), types[i], names[i], xStart + i * dx, y);
        }
        return nodes;
    }
}
