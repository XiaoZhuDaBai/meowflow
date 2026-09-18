package com.meowflow.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.meowflow")
public class MeowFlowWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeowFlowWorkflowApplication.class, args);
    }
}

