package org.dragonfly.ddtd.spring;

import org.dragonfly.ddtd.conf.GlobalProperties;
import org.dragonfly.ddtd.framework.TaskZooKeeperFactory;
import org.dragonfly.ddtd.framework.inspect.DefaultTaskInspector;
import org.dragonfly.ddtd.framework.inspect.ITaskInspect;
import org.dragonfly.ddtd.framework.report.DefaultTaskReporter;
import org.dragonfly.ddtd.framework.report.ITaskReporter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Configurations of ddtd for bootstrap.
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/11/25
 **/
public class DdtdBootstrapConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "dragonfly-ddtd")
    public GlobalProperties getGlobalProperties() {
        return new GlobalProperties();
    }

    @Bean(initMethod = "initialize", destroyMethod = "destroy")
    public TaskZooKeeperFactory getTaskZooKeeperFactory(GlobalProperties globalProperties) {
        return new TaskZooKeeperFactory(globalProperties);
    }

    @Bean
    public ITaskReporter getTaskReporter() {
        return new DefaultTaskReporter();
    }

    @Bean
    public ITaskInspect getTaskInspect(TaskZooKeeperFactory taskZooKeeperFactory) {
        return new DefaultTaskInspector(taskZooKeeperFactory);
    }

    @Bean
    public DdtdAutoFactoryRegistrar getAutoFactoryRegistrar(
            ApplicationContext applicationContext,
            GlobalProperties globalProperties,
            TaskZooKeeperFactory taskZooKeeperFactory,
            ITaskReporter taskReporter,
            ITaskInspect taskInspect
    ) {
        return new DdtdAutoFactoryRegistrar(
                applicationContext,
                globalProperties,
                taskZooKeeperFactory,
                taskReporter,
                taskInspect
        );
    }
}
