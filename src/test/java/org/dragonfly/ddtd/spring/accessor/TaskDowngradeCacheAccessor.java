package org.dragonfly.ddtd.spring.accessor;

import org.springframework.stereotype.Component;

/**
 * 作用描述 直接降级
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/2/10
 **/
@Component("taskDowngradeCacheAccessor")
public class TaskDowngradeCacheAccessor extends TaskDistributionCacheAccessor {

    @Override
    public boolean isDistributed() {
        return false;
    }
}
