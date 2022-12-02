package org.dragonfly.ddtd.spring.accessor;

import org.dragonfly.ddtd.framework.entity.ITaskContext;
import org.dragonfly.ddtd.spring.domain.DdtdExampleTaskDO;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;

/**
 * 作用描述 N次取数后，降级不可用
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/2/9
 **/
@Component("taskDowngradeDistributionCacheAccessor")
public class TaskDowngradeDistributionCacheAccessor extends TaskDistributionCacheAccessor {

    private final LongAdder counter = new LongAdder();

    public TaskDowngradeDistributionCacheAccessor() {
        this.counter.add(5);
    }

    @Override
    public DdtdExampleTaskDO pollOne(ITaskContext context) {
        this.counter.decrement();
        return super.pollOne(context);
    }

    @Override
    public boolean isDistributed() {
        return counter.intValue() > 0;
    }
}
