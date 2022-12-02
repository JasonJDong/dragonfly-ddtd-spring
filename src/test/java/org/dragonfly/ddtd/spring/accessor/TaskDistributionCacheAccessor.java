package org.dragonfly.ddtd.spring.accessor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assertj.core.util.Lists;
import org.dragonfly.ddtd.framework.entity.ITaskContext;
import org.dragonfly.ddtd.framework.task.ITaskCacheableDataAccessor;
import org.dragonfly.ddtd.spring.domain.DdtdExampleTaskDO;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Cached data access
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/2/8
 **/
@Slf4j
@SuppressWarnings("unchecked")
@Component("taskDistributionCacheAccessor")
public class TaskDistributionCacheAccessor implements ITaskCacheableDataAccessor<DdtdExampleTaskDO> {

    private final Cache<String, Object> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1L, TimeUnit.DAYS)
            .build();

    public TaskDistributionCacheAccessor() {
    }

    private String getKey(ITaskContext context) {
        return String.format("dragonfly-ddtd-test:%s", context.getTaskId());
    }

    @Override
    public void cacheAllData(ITaskContext context, Collection<DdtdExampleTaskDO> data) throws Exception {
        try {
            final List<DdtdExampleTaskDO> cachedData =
                    (List<DdtdExampleTaskDO>) cache.get(getKey(context), CopyOnWriteArrayList::new);
            cachedData.addAll(data);
        } catch (Exception e) {
            log.error("将list放入缓存发生异常，key：{}，exception：{}", getKey(context), ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    @Override
    public boolean exists(ITaskContext context) {
        try {
            final List<DdtdExampleTaskDO> cachedData = (List<DdtdExampleTaskDO>) cache.getIfPresent(getKey(context));
            return CollectionUtils.isNotEmpty(cachedData);
        } catch (Exception e) {
            log.error("判断key是否存在发生异常，key：{}，exception：{}", getKey(context), ExceptionUtils.getStackTrace(e));
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public DdtdExampleTaskDO pollOne(ITaskContext context) {
        try {
            final List<DdtdExampleTaskDO> cachedData =
                    (List<DdtdExampleTaskDO>) cache.getIfPresent(getKey(context));
            if (CollectionUtils.isEmpty(cachedData)) {
                return null;
            }
            return cachedData.remove(0);
        } catch (Exception e) {
            log.error("list缓存的第一个内容取出队列发生异常，key：{}，exception：{}", getKey(context), ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

    @Override
    public void clear(ITaskContext context) {
        try {
            final List<DdtdExampleTaskDO> cachedData =
                    (List<DdtdExampleTaskDO>) cache.getIfPresent(getKey(context));
            if (CollectionUtils.isEmpty(cachedData)) {
                return;
            }
            cachedData.clear();
        } catch (Exception e) {
            log.error("清理时发生异常，key：{}，exception：{}", getKey(context), ExceptionUtils.getStackTrace(e));
        }
    }
}
