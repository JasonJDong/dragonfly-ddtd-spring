package org.dragonfly.ddtd.spring.accessor;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.LongAdder;

/**
 * 作用描述 升降升
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/2/11
 **/
@Component("taskUpDownUpDistributedCacheAccessor")
public class TaskUpDownUpDistributedCacheAccessor extends TaskDistributionCacheAccessor {

    private LongAdder counter = new LongAdder();

    public TaskUpDownUpDistributedCacheAccessor() {
        counter.add(10);
    }

    @Override
    public boolean isDistributed() {
        if (counter != null) {
            counter.decrement();
        }
        if (counter == null || counter.intValue() > 0) {
            return true;
        }
        if (counter != null && counter.intValue() > -5 && counter.intValue() < 0) {
            return false;
        }
        counter = null;
        return true;
    }
}
