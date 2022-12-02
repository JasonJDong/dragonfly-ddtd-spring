package org.dragonfly.ddtd.spring.context;

import org.dragonfly.ddtd.framework.entity.ITaskContext;
import org.dragonfly.ddtd.enums.TaskDistributionStrategy;
import lombok.Data;

/**
 * 作用描述 测试任务上下文
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/2/8
 **/
@Data
public class TestTaskContext implements ITaskContext {

    private final String tenant;
    private final String taskId;

    private TaskDistributionStrategy strategy = TaskDistributionStrategy.DYNAMIC;

    public TestTaskContext(String tenant, String taskId) {
        this.tenant = tenant;
        this.taskId = taskId;
    }

    @Override
    public TaskDistributionStrategy getStrategy() {
        return strategy;
    }

    @Override
    public Integer getFixedCount() {
        return 4;
    }
}
