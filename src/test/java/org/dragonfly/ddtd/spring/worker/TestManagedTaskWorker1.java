package org.dragonfly.ddtd.spring.worker;

import cn.hutool.core.util.RandomUtil;
import org.apache.commons.lang3.StringUtils;
import org.dragonfly.ddtd.enums.TaskWorkState;
import org.dragonfly.ddtd.framework.entity.ITaskContext;
import org.dragonfly.ddtd.framework.task.ITaskDataAccessor;
import org.dragonfly.ddtd.framework.task.ITaskWorker;
import org.dragonfly.ddtd.spring.context.TestTaskContext;
import org.dragonfly.ddtd.spring.domain.DdtdExampleTaskDO;
import org.springframework.stereotype.Component;

/**
 * Managed by Spring, no use to actual distributed test
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/11/29
 **/
@Component
public class TestManagedTaskWorker1 implements ITaskWorker<DdtdExampleTaskDO> {
    @Override
    public boolean accept(ITaskContext context) {
        return false;
    }

    @Override
    public ITaskDataAccessor<DdtdExampleTaskDO> getTaskDataAccessor() {
        return null;
    }

    @Override
    public ITaskContext parseContext(String rawContext) {
        return new TestTaskContext("t3", RandomUtil.randomString(16));
    }

    @Override
    public TaskWorkState partitionWork(ITaskContext context, DdtdExampleTaskDO data) {
        return TaskWorkState.SUCCESS;
    }

    @Override
    public TaskWorkState cleanupWork(ITaskContext context) {
        return TaskWorkState.SUCCESS;
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public String getName() {
        return StringUtils.uncapitalize(getClass().getSimpleName());
    }

    @Override
    public String getDescription() {
        return "description";
    }
}
