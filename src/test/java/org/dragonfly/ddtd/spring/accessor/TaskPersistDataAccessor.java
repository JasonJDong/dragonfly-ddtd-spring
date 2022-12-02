package org.dragonfly.ddtd.spring.accessor;

import cn.hutool.core.net.NetUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import org.dragonfly.ddtd.spring.context.TestTaskContext;
import org.dragonfly.ddtd.framework.entity.ITaskContext;
import org.dragonfly.ddtd.framework.task.ITaskPersistDataAccessor;
import org.dragonfly.ddtd.spring.domain.DdtdExampleTaskDO;
import org.dragonfly.ddtd.spring.domain.enums.TaskProcessState;
import org.dragonfly.ddtd.enums.TaskWorkState;
import org.dragonfly.ddtd.spring.service.IDdtdExampleTaskService;
import org.dragonfly.ddtd.utils.RuntimeUtil;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * 作用描述 持久化任务数据访问
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/2/8
 **/
@Component
public class TaskPersistDataAccessor implements ITaskPersistDataAccessor<DdtdExampleTaskDO> {

    private final IDdtdExampleTaskService exampleTaskService;

    public TaskPersistDataAccessor(IDdtdExampleTaskService exampleTaskService) {
        this.exampleTaskService = exampleTaskService;
    }

    @Override
    public void persist(ITaskContext context, Collection<DdtdExampleTaskDO> data) {

        if (CollectionUtils.isEmpty(data)) {
            return;
        }
        exampleTaskService.saveOrUpdateBatch(data);
    }

    @Override
    public void terminate(ITaskContext context) {
        new LambdaUpdateChainWrapper<>(exampleTaskService.getBaseMapper())
                .eq(DdtdExampleTaskDO::getTaskId, context.getTaskId())
                .isNull(DdtdExampleTaskDO::getState)
                .set(DdtdExampleTaskDO::getState, TaskProcessState.TERMINATE.getValue())
                .set(DdtdExampleTaskDO::getModified, new Date())
                .update();
    }

    @Override
    public void resolved(ITaskContext context, DdtdExampleTaskDO one, TaskWorkState state) {

        if (one == null) {
            return;
        }

        if (one.getId() == null) {
            throw new IllegalArgumentException("数据没有Id");
        }
        if (state == TaskWorkState.PASS) {
            return;
        }
        int processState = state == TaskWorkState.SUCCESS ?
                TaskProcessState.SUCCESS.getValue() : TaskProcessState.FAIL.getValue();

        String elapsed = null;
        if (one.getStartAt() != null) {
            elapsed = DurationFormatUtils.formatDurationHMS(
                    System.currentTimeMillis() - one.getStartAt().getTime()
            );
        }

        Map<String, Object> instInfo = Maps.newHashMap();
        instInfo.put("machine", NetUtil.getLocalhostStr());
        instInfo.put("thread", Thread.currentThread().getName());
        instInfo.put("runtime", new String[]{RuntimeUtil.cpuLoadString(), RuntimeUtil.memoryLoadString()});


        new LambdaUpdateChainWrapper<>(exampleTaskService.getBaseMapper())
                .eq(DdtdExampleTaskDO::getId, one.getId())
                .set(DdtdExampleTaskDO::getState, processState)
                .set(DdtdExampleTaskDO::getInstInfo, JSON.toJSONString(instInfo))
                .set(DdtdExampleTaskDO::getStartAt, one.getStartAt())
                .set(elapsed != null, DdtdExampleTaskDO::getElapsed, elapsed)
                .set(DdtdExampleTaskDO::getModified, new Date())
                .update();
    }

    @Override
    public boolean existsUnresolved(ITaskContext context) {
        return new LambdaQueryChainWrapper<>(exampleTaskService.getBaseMapper())
                .eq(DdtdExampleTaskDO::getTaskId, context.getTaskId())
                .isNull(DdtdExampleTaskDO::getState)
                .exists();
    }

    @Override
    public Collection<DdtdExampleTaskDO> allUnresolved(ITaskContext context) {
        return new LambdaQueryChainWrapper<>(exampleTaskService.getBaseMapper())
                .eq(DdtdExampleTaskDO::getTaskId, context.getTaskId())
                .eq(DdtdExampleTaskDO::getTenant, ((TestTaskContext) context).getTenant())
                .isNull(DdtdExampleTaskDO::getState)
                .list();
    }

    @Override
    public void clear(ITaskContext context) {
        LambdaQueryWrapper<DdtdExampleTaskDO> delWrapper = Wrappers.<DdtdExampleTaskDO>lambdaQuery()
                .eq(DdtdExampleTaskDO::getTaskId, context.getTaskId());
        exampleTaskService.remove(delWrapper);
    }
}
