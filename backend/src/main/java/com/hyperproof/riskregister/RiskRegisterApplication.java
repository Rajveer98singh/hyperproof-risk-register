package com.hyperproof.riskregister;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Hyperproof Risk Register application entry point.
 *
 * <p>{@code @EnableRetry} activates the optimistic-lock retry on concurrent risk updates
 * (see {@code RiskService}).
 */
@SpringBootApplication
@EnableRetry
public class RiskRegisterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskRegisterApplication.class, args);
    }
}
