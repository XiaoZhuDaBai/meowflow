package com.meowflow.common.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ID 生成器工厂
 * <p>
 * 统一管理 IdGenerator 实例，支持通过 workerId 初始化
 */
@Slf4j
@Component
public class IdGeneratorFactory {

    private IdGenerator idGenerator;

    @PostConstruct
    public void init() {
        long workerId = WorkerIdAssigner.getWorkerIdOrDefault(0);
        idGenerator = new IdGenerator(workerId);
        log.info("IdGenerator 初始化完成，workerId: {}", workerId);
    }

    /**
     * 获取 ID 生成器实例
     *
     * @return IdGenerator
     */
    public IdGenerator getIdGenerator() {
        if (idGenerator == null) {
            synchronized (this) {
                if (idGenerator == null) {
                    long workerId = WorkerIdAssigner.getWorkerIdOrDefault(0);
                    idGenerator = new IdGenerator(workerId);
                }
            }
        }
        return idGenerator;
    }

    /**
     * 生成下一个 ID
     *
     * @return 唯一 ID
     */
    public long nextId() {
        return getIdGenerator().nextId();
    }

    /**
     * 生成下一个 ID（字符串形式）
     *
     * @return 唯一 ID 字符串
     */
    public String nextIdStr() {
        return getIdGenerator().nextIdStr();
    }
}
