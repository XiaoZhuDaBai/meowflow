package com.meowflow.infra.embedding;

/**
 * Embedding 结果
 */
public class EmbeddingResult {

    private final String text;
    private final float[] vector;
    private final int tokens;
    private final long latencyMs;

    public EmbeddingResult(String text, float[] vector, int tokens, long latencyMs) {
        this.text = text;
        this.vector = vector;
        this.tokens = tokens;
        this.latencyMs = latencyMs;
    }

    public String getText() {
        return text;
    }

    public float[] getVector() {
        return vector;
    }

    public int getTokens() {
        return tokens;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public int getDimension() {
        return vector != null ? vector.length : 0;
    }
}
