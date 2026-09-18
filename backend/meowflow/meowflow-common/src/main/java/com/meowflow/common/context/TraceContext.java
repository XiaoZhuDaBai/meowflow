package com.meowflow.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String traceId;
    private String spanId;
    private String parentSpanId;
    private long startTime;
    private Map<String, String> tags = new HashMap<>();
}