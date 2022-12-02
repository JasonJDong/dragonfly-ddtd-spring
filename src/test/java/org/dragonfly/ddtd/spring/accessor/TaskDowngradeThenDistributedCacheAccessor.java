package org.dragonfly.ddtd.spring.accessor;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;

/**
 * 作用描述 先降级后升级
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/2/10
 **/
@Component("taskDowngradeThenDistributedCacheAccessor")
public class TaskDowngradeThenDistributedCacheAccessor extends TaskDistributionCacheAccessor {

    private final LongAdder counter = new LongAdder();

    public TaskDowngradeThenDistributedCacheAccessor() {
        this.counter.add(5);
    }

    @Override
    public boolean isDistributed() {
        this.counter.decrement();
        return counter.intValue() < 0;
    }
}
