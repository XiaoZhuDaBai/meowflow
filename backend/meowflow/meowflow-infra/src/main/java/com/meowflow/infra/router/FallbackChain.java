package com.meowflow.infra.router;

import java.util.List;

/**
 * 降级链 - 定义模型降级策略
 */
public class FallbackChain {

    private final String name;
    private final List<String> chain;
    private int currentIndex;

    public FallbackChain(String name, List<String> chain) {
        this.name = name;
        this.chain = chain;
        this.currentIndex = 0;
    }

    public String getCurrent() {
        if (currentIndex < chain.size()) {
            return chain.get(currentIndex);
        }
        return chain.isEmpty() ? null : chain.get(0);
    }

    public String next() {
        if (currentIndex < chain.size() - 1) {
            currentIndex++;
            return chain.get(currentIndex);
        }
        return null;
    }

    public void reset() {
        currentIndex = 0;
    }

    public boolean hasNext() {
        return currentIndex < chain.size() - 1;
    }

    public int getRemainingCount() {
        return chain.size() - currentIndex;
    }

    public List<String> getChain() {
        return chain;
    }

    public String getName() {
        return name;
    }
}
