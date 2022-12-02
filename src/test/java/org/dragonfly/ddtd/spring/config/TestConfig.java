package org.dragonfly.ddtd.spring.config;

import org.dragonfly.ddtd.spring.DdtdBootstrapConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 作用描述 测试配置
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/2/8
 **/
@Configuration
@Import(DdtdBootstrapConfiguration.class)
public class TestConfig {
}
