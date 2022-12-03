package org.dragonfly.ddtd.spring.test;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dragonfly.ddtd.conf.GlobalProperties;
import org.dragonfly.ddtd.enums.TaskWorkState;
import org.dragonfly.ddtd.framework.TaskZooKeeperFactory;
import org.dragonfly.ddtd.framework.entity.ITaskContext;
import org.dragonfly.ddtd.framework.inspect.ITaskInspect;
import org.dragonfly.ddtd.framework.lifecycle.ITaskLifeCycleHook;
import org.dragonfly.ddtd.framework.report.ITaskReporter;
import org.dragonfly.ddtd.framework.task.*;
import org.dragonfly.ddtd.spring.DdtdAutoFactoryRegistrar;
import org.dragonfly.ddtd.spring.context.TestTaskContext;
import org.dragonfly.ddtd.spring.domain.DdtdExampleTaskDO;
import org.dragonfly.ddtd.spring.domain.enums.TaskProcessState;
import org.dragonfly.ddtd.spring.service.IDdtdExampleTaskService;
import org.dragonfly.ddtd.spring.worker.TestManagedTaskWorker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 作用描述 分布式任务单元测试
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/2/8
 **/
@Slf4j
@SpringBootTest
@MapperScan(basePackages = {"org.dragonfly.ddtd.spring.mapper"})
public class TestDistributionTask {

    @Autowired
    private GlobalProperties globalProperties;
    @Autowired
    private TaskZooKeeperFactory taskZooKeeperFactory;
    @Autowired
    private ITaskInspect taskInspect;
    @Autowired
    private ITaskReporter taskReporter;
    @Autowired
    private ITaskPersistDataAccessor<DdtdExampleTaskDO> taskPersistDataAccessor;
    @Autowired
    private IDdtdExampleTaskService cxPeTaskService;
    @Autowired
    private DdtdAutoFactoryRegistrar autoFactoryRegistrar;

    @Autowired
    @Qualifier("taskDistributionCacheAccessor")
    private ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskDistributionDataAccessor;
    @Autowired
    @Qualifier("taskDowngradeDistributionCacheAccessor")
    private ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskDowngradeDistributionCacheAccessor;
    @Autowired
    @Qualifier("taskDowngradeCacheAccessor")
    private ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskDowngradeCacheAccessor;
    @Autowired
    @Qualifier("taskDowngradeThenDistributedCacheAccessor")
    private ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskDowngradeThenDistributedCacheAccessor;
    @Autowired
    @Qualifier("taskUpDownUpDistributedCacheAccessor")
    private ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskUpDownUpDistributedCacheAccessor;

    @Test
    public void checkPropertiesTest() {
        Assertions.assertNotNull(globalProperties);
        Assertions.assertNotNull(globalProperties.getDowngrade());
        Assertions.assertNotNull(globalProperties.getZookeeperAddr());
        Assertions.assertNotNull(globalProperties.getNamespace());
        Assertions.assertNotNull(globalProperties.getGroup());

        log.info(JSON.toJSONString(globalProperties, SerializerFeature.PrettyFormat));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("自动注册任务工厂测试")
    public void AutoFactoryRegistrarTest() throws Exception {
        final DefaultTaskFactory<DdtdExampleTaskDO> factory = (DefaultTaskFactory<DdtdExampleTaskDO>) autoFactoryRegistrar.getFactory(TestManagedTaskWorker.class);
        final ITaskWorker<DdtdExampleTaskDO> worker1 = createWorker1(taskDistributionDataAccessor, null);
        final ITaskWorker<DdtdExampleTaskDO> worker2 = createWorker1(taskDistributionDataAccessor, null);
        autoFactoryRegistrar.register(worker1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            autoFactoryRegistrar.register(createWorker1(taskDistributionDataAccessor, null));
        });
        autoFactoryRegistrar.register("worker2", worker2);
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            autoFactoryRegistrar.register("worker2", createWorker1(taskDistributionDataAccessor, null));
        });

        final DefaultTaskFactory<Object> factory1 = autoFactoryRegistrar.getFactory(worker1.getName(), worker1.getClass());
        final DefaultTaskFactory<Object> factory2 = autoFactoryRegistrar.getFactory("worker2", worker2.getClass());

        Assertions.assertNotNull(factory);
        Assertions.assertNotNull(factory1);
        Assertions.assertNotNull(factory2);
    }

    @Test
    @DisplayName("分布式调度处理测试")
    public void distributionTaskTest() throws Exception {
        this.simulate(taskDistributionDataAccessor);
    }

    @Test
    @DisplayName("分布式调度处理降级测试")
    public void distributionDowngradeTaskTest() throws Exception {
        this.simulate(taskDowngradeDistributionCacheAccessor);
    }

    @Test
    @DisplayName("分布式调度直接降级测试")
    public void directDowngradeTaskTest() throws Exception {
        this.simulate(taskDowngradeCacheAccessor);
    }

    @Test
    @DisplayName("分布式调度降级后升级测试")
    public void directDowngradeUpgradeTaskTest() throws Exception {
        this.simulate(taskDowngradeThenDistributedCacheAccessor);
    }

    @Test
    @DisplayName("升降升测试")
    public void upDownUpgradeTaskTest() throws Exception {
        this.simulate(taskUpDownUpDistributedCacheAccessor);
    }

    @Test
    @DisplayName("模拟同任务多上下文同时执行测试")
    public void multipleSameTaskTest() throws Exception {
        CountDownLatch waiter = new CountDownLatch(2);

        Consumer<TestTaskContext> onCompleted = (ctx) -> {
            log.info("{} - {} 所有任务处理完成", ctx.getTenant(), ctx.getTaskId());
            waiter.countDown();
        };
        ITaskWorker<DdtdExampleTaskDO> taskWorker = createWorker1(taskDistributionDataAccessor, onCompleted);

        DefaultTaskFactory<DdtdExampleTaskDO> masterFactory = createFactory(taskWorker);
        List<DefaultTaskFactory<DdtdExampleTaskDO>> factories = Lists.newArrayListWithExpectedSize(8);

        TestTaskContext context1 = new TestTaskContext(
                "t1",
                RandomUtil.randomString(3) + System.currentTimeMillis()
        );

        TestTaskContext context2 = new TestTaskContext(
                "t2",
                RandomUtil.randomString(3) + System.currentTimeMillis()
        );
        try {

            // 模拟启动多个实例
            for (int i = 0; i < 7; i++) {
                DefaultTaskFactory<DdtdExampleTaskDO> taskFactory = createFactory(taskWorker);
                log.info("{}号模拟JVM处理器正常启动", i + 1);
                factories.add(taskFactory);
            }

            // 模拟拆分任务
            List<DdtdExampleTaskDO> data1 = IntStream.range(0, 10)
                    .mapToObj(index -> {
                        DdtdExampleTaskDO taskDO = new DdtdExampleTaskDO();
                        taskDO.setTenant(context1.getTenant());
                        taskDO.setTaskData(JSON.toJSONString(new String[]{String.format("%04d", index)}));
                        taskDO.setTaskId(context1.getTaskId());
                        return taskDO;
                    })
                    .collect(Collectors.toList());

            // 模拟拆分任务
            List<DdtdExampleTaskDO> data2 = IntStream.range(0, 40)
                    .mapToObj(index -> {
                        DdtdExampleTaskDO taskDO = new DdtdExampleTaskDO();
                        taskDO.setTenant(context2.getTenant());
                        taskDO.setTaskData(JSON.toJSONString(new String[]{String.format("%04d", index)}));
                        taskDO.setTaskId(context2.getTaskId());
                        return taskDO;
                    })
                    .collect(Collectors.toList());
            masterFactory.applyTask(context1, data1);
            log.info("模拟JVM启动任务1");
            masterFactory.applyTask(context2, data2);
            log.info("模拟JVM启动任务2");

        } catch (Exception e) {
            log.error("任务启动异常", e);
            waiter.countDown();
        }
        waiter.await();
        masterFactory.destroy();
        for (DefaultTaskFactory<DdtdExampleTaskDO> factory : factories) {
            factory.destroy();
        }
        Assertions.assertFalse(taskPersistDataAccessor.existsUnresolved(context1));
        Assertions.assertFalse(taskPersistDataAccessor.existsUnresolved(context2));

    }

    @Test
    @DisplayName("模拟不同任务同时执行测试")
    public void multipleDiffTaskTest() throws Exception {
        CountDownLatch waiter = new CountDownLatch(2);

        Consumer<TestTaskContext> onCompleted = (ctx) -> {
            log.info("{} - {} 所有任务处理完成", ctx.getTenant(), ctx.getTaskId());
            waiter.countDown();
        };
        ITaskWorker<DdtdExampleTaskDO> taskWorker1 = createWorker1(taskDistributionDataAccessor, onCompleted);
        ITaskWorker<DdtdExampleTaskDO> taskWorker2 = createWorker2(taskDistributionDataAccessor, onCompleted);

        DefaultTaskFactory<DdtdExampleTaskDO> masterFactory1 = createFactory(taskWorker1);
        DefaultTaskFactory<DdtdExampleTaskDO> masterFactory2 = createFactory(taskWorker2);
        List<DefaultTaskFactory<DdtdExampleTaskDO>> factories = Lists.newArrayListWithExpectedSize(16);

        TestTaskContext context1 = new TestTaskContext(
                "t1",
                RandomUtil.randomString(3) + System.currentTimeMillis()
        );

        TestTaskContext context2 = new TestTaskContext(
                "t2",
                RandomUtil.randomString(3) + System.currentTimeMillis()
        );
        try {

            // 模拟启动多个实例
            for (int i = 0; i < 7; i++) {
                DefaultTaskFactory<DdtdExampleTaskDO> taskFactory1 = createFactory(taskWorker1);
                log.info("{}号factory1模拟JVM处理器正常启动", i + 1);
                DefaultTaskFactory<DdtdExampleTaskDO> taskFactory2 = createFactory(taskWorker2);
                log.info("{}号factory2模拟JVM处理器正常启动", i + 1);
                factories.add(taskFactory1);
                factories.add(taskFactory2);
            }

            // 模拟拆分任务
            List<DdtdExampleTaskDO> data1 = IntStream.range(0, 20)
                    .mapToObj(index -> {
                        DdtdExampleTaskDO taskDO = new DdtdExampleTaskDO();
                        taskDO.setTenant(context1.getTenant());
                        taskDO.setTaskData(JSON.toJSONString(new String[]{String.format("%04d", index)}));
                        taskDO.setTaskId(context1.getTaskId());
                        return taskDO;
                    })
                    .collect(Collectors.toList());

            // 模拟拆分任务
            List<DdtdExampleTaskDO> data2 = IntStream.range(0, 20)
                    .mapToObj(index -> {
                        DdtdExampleTaskDO taskDO = new DdtdExampleTaskDO();
                        taskDO.setTenant(context2.getTenant());
                        taskDO.setTaskData(JSON.toJSONString(new String[]{String.format("%04d", index)}));
                        taskDO.setTaskId(context2.getTaskId());
                        return taskDO;
                    })
                    .collect(Collectors.toList());
            masterFactory1.applyTask(context1, data1);
            log.info("模拟JVM启动任务1");
            masterFactory2.applyTask(context2, data2);
            log.info("模拟JVM启动任务2");

        } catch (Exception e) {
            log.error("任务启动异常", e);
            waiter.countDown();
        }
        waiter.await();
        masterFactory1.destroy();
        masterFactory2.destroy();
        for (DefaultTaskFactory<DdtdExampleTaskDO> factory : factories) {
            factory.destroy();
        }
        Assertions.assertFalse(taskPersistDataAccessor.existsUnresolved(context1));
        Assertions.assertFalse(taskPersistDataAccessor.existsUnresolved(context2));

    }

    @Test
    @DisplayName("终止测试")
    public void terminateTaskTest() throws Exception {

        CountDownLatch waiter = new CountDownLatch(1);

        Consumer<TestTaskContext> onCompleted = (ctx) -> {
            log.info("{} - {} 所有任务处理完成", ctx.getTenant(), ctx.getTaskId());
            waiter.countDown();
        };
        ITaskWorker<DdtdExampleTaskDO> taskWorker = createWorker1(taskDistributionDataAccessor, onCompleted);

        DefaultTaskFactory<DdtdExampleTaskDO> masterFactory = createFactory(taskWorker);
        List<DefaultTaskFactory<DdtdExampleTaskDO>> factories = Lists.newArrayListWithExpectedSize(8);

        TestTaskContext context = new TestTaskContext(
                "t1",
                RandomUtil.randomString(3) + System.currentTimeMillis()
        );
        try {

            LongAdder counter = new LongAdder();
            ITaskLifeCycleHook cycleHook = new ITaskLifeCycleHook() {
                @Override
                public void onEachTaskStarted(ITaskWorker<?> worker, ITaskContext taskContext, Object taskData) {
                    counter.increment();
                    if (counter.intValue() > 10) {
                        log.info("================== 终止处理");
                        masterFactory.terminateTask((ITaskWorker<DdtdExampleTaskDO>) worker, context);
                    }
                }
            };

            // 模拟启动多个实例
            for (int i = 0; i < 7; i++) {
                DefaultTaskFactory<DdtdExampleTaskDO> taskFactory = createFactory(taskWorker);
                taskFactory.setTaskHandleCallback(cycleHook);
                log.info("{}号模拟JVM处理器正常启动", i + 1);
                factories.add(taskFactory);
            }

            // 模拟拆分任务
            List<DdtdExampleTaskDO> data = IntStream.range(0, 50)
                    .mapToObj(index -> {
                        DdtdExampleTaskDO taskDO = new DdtdExampleTaskDO();
                        taskDO.setTenant(context.getTenant());
                        taskDO.setTaskData(JSON.toJSONString(new String[]{String.format("%04d", index)}));
                        taskDO.setTaskId(context.getTaskId());
                        return taskDO;
                    })
                    .collect(Collectors.toList());

            masterFactory.setTaskHandleCallback(cycleHook);
            masterFactory.applyTask(context, data);
            log.info("模拟JVM启动任务");

        } catch (Exception e) {
            log.error("任务启动异常", e);
            waiter.countDown();
        }
        waiter.await();
        masterFactory.destroy();
        for (DefaultTaskFactory<DdtdExampleTaskDO> factory : factories) {
            factory.destroy();
        }

        boolean exists = new LambdaQueryChainWrapper<>(cxPeTaskService.getBaseMapper())
                .eq(DdtdExampleTaskDO::getTaskId, context.getTaskId())
                .eq(DdtdExampleTaskDO::getState, TaskProcessState.TERMINATE.getValue())
                .exists();
        Assertions.assertTrue(exists);
    }


    private void simulate(ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskCacheableDataAccessor) throws Exception {

        CountDownLatch waiter = new CountDownLatch(1);

        Consumer<TestTaskContext> onCompleted = (ctx) -> {
            log.info("{} - {} 所有任务处理完成", ctx.getTenant(), ctx.getTaskId());
            waiter.countDown();
        };
        ITaskWorker<DdtdExampleTaskDO> taskWorker = createWorker1(taskCacheableDataAccessor, onCompleted);

        DefaultTaskFactory<DdtdExampleTaskDO> masterFactory = createFactory(taskWorker);
        List<DefaultTaskFactory<DdtdExampleTaskDO>> factories = Lists.newArrayListWithExpectedSize(8);

        TestTaskContext context = new TestTaskContext(
                "t1",
                RandomUtil.randomString(3) + System.currentTimeMillis()
        );
        try {

            // 模拟启动多个实例
            for (int i = 0; i < 7; i++) {
                DefaultTaskFactory<DdtdExampleTaskDO> taskFactory = createFactory(taskWorker);
                log.info("{}号模拟JVM处理器正常启动", i + 1);
                factories.add(taskFactory);
            }

            // 模拟拆分任务
            List<DdtdExampleTaskDO> data = IntStream.range(0, 20)
                    .mapToObj(index -> {
                        DdtdExampleTaskDO taskDO = new DdtdExampleTaskDO();
                        taskDO.setTenant(context.getTenant());
                        taskDO.setTaskData(JSON.toJSONString(new String[]{String.format("%04d", index)}));
                        taskDO.setTaskId(context.getTaskId());
                        return taskDO;
                    })
                    .collect(Collectors.toList());
            masterFactory.applyTask(context, data);
            log.info("模拟JVM启动任务");

        } catch (Exception e) {
            log.error("任务启动异常", e);
            waiter.countDown();
        }
        waiter.await();
        masterFactory.destroy();
        for (DefaultTaskFactory<DdtdExampleTaskDO> factory : factories) {
            factory.destroy();
        }
        Assertions.assertFalse(taskPersistDataAccessor.existsUnresolved(context));
    }


    private DefaultTaskFactory<DdtdExampleTaskDO> createFactory(ITaskWorker<DdtdExampleTaskDO> taskWorker) throws Exception {

        // 任务工厂
        DefaultTaskFactory<DdtdExampleTaskDO> taskFactory = new DefaultTaskFactory<>(
                globalProperties,
                taskZooKeeperFactory,
                taskWorker,
                taskInspect,
                taskReporter
        );
        taskFactory.initialize();
        return taskFactory;
    }

    private ITaskWorker<DdtdExampleTaskDO> createWorker1(ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskCacheableDataAccessor,
                                                         Consumer<TestTaskContext> onCompleted) {
        TestWorker1 testWorker = new TestWorker1(
                taskCacheableDataAccessor,
                taskPersistDataAccessor
        );
        testWorker.setOnCompleted(onCompleted);
        return testWorker;
    }

    private ITaskWorker<DdtdExampleTaskDO> createWorker2(ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskCacheableDataAccessor,
                                                         Consumer<TestTaskContext> onCompleted) {
        TestWorker2 testWorker = new TestWorker2(
                taskCacheableDataAccessor,
                taskPersistDataAccessor
        );
        testWorker.setOnCompleted(onCompleted);
        return testWorker;
    }


    static class TestWorker1 implements ITaskWorker<DdtdExampleTaskDO> {

        private final ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskCacheableDataAccessor;

        private final ITaskPersistDataAccessor<DdtdExampleTaskDO> taskPersistDataAccessor;

        @Getter
        @Setter
        private Consumer<TestTaskContext> onCompleted;

        TestWorker1(ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskCacheableDataAccessor,
                    ITaskPersistDataAccessor<DdtdExampleTaskDO> taskPersistDataAccessor) {
            this.taskCacheableDataAccessor = taskCacheableDataAccessor;
            this.taskPersistDataAccessor = taskPersistDataAccessor;
        }

        @Override
        public boolean accept(ITaskContext context) {
            return true;
        }

        @Override
        public ITaskDataAccessor<DdtdExampleTaskDO> getTaskDataAccessor() {
            return new ITaskDataAccessor<DdtdExampleTaskDO>() {
                @Override
                public ITaskCacheableDataAccessor<DdtdExampleTaskDO> getTaskCacheableDataAccessor() {
                    return taskCacheableDataAccessor;
                }

                @Override
                public ITaskPersistDataAccessor<DdtdExampleTaskDO> getTaskPersistDataAccessor() {
                    return taskPersistDataAccessor;
                }
            };
        }

        @Override
        public ITaskContext parseContext(String rawContext) {
            return JSON.parseObject(rawContext, TestTaskContext.class);
        }

        @Override
        public TaskWorkState partitionWork(ITaskContext context, DdtdExampleTaskDO data) {

            try {
                if (data == null) {
                    return TaskWorkState.PASS;
                }
                data.setStartAt(new Date());
                TimeUnit.SECONDS.sleep(5);
                return TaskWorkState.SUCCESS;
            } catch (InterruptedException e) {
                log.info("任务中断。。。");
                return TaskWorkState.FAIL;
            }
        }

        @Override
        public TaskWorkState cleanupWork(ITaskContext context) {
            try {
                TimeUnit.SECONDS.sleep(5L);
                if (onCompleted != null) {
                    onCompleted.accept((TestTaskContext) context);
                }
                return TaskWorkState.SUCCESS;
            } catch (InterruptedException e) {
                log.info("清理中断。。。");
                return TaskWorkState.FAIL;
            }
        }

        @Override
        public String getVersion() {
            return "1";
        }

        @Override
        public String getName() {
            return "TestWorker1";
        }

        @Override
        public String getDescription() {
            return "测试分布式任务2";
        }
    }

    static class TestWorker2 implements ITaskWorker<DdtdExampleTaskDO> {

        private final ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskCacheableDataAccessor;

        private final ITaskPersistDataAccessor<DdtdExampleTaskDO> taskPersistDataAccessor;

        @Getter
        @Setter
        private Consumer<TestTaskContext> onCompleted;

        TestWorker2(ITaskCacheableDataAccessor<DdtdExampleTaskDO> taskCacheableDataAccessor,
                    ITaskPersistDataAccessor<DdtdExampleTaskDO> taskPersistDataAccessor) {
            this.taskCacheableDataAccessor = taskCacheableDataAccessor;
            this.taskPersistDataAccessor = taskPersistDataAccessor;
        }

        @Override
        public boolean accept(ITaskContext context) {
            return true;
        }

        @Override
        public ITaskDataAccessor<DdtdExampleTaskDO> getTaskDataAccessor() {
            return new ITaskDataAccessor<DdtdExampleTaskDO>() {
                @Override
                public ITaskCacheableDataAccessor<DdtdExampleTaskDO> getTaskCacheableDataAccessor() {
                    return taskCacheableDataAccessor;
                }

                @Override
                public ITaskPersistDataAccessor<DdtdExampleTaskDO> getTaskPersistDataAccessor() {
                    return taskPersistDataAccessor;
                }
            };
        }

        @Override
        public ITaskContext parseContext(String rawContext) {
            return JSON.parseObject(rawContext, TestTaskContext.class);
        }

        @Override
        public TaskWorkState partitionWork(ITaskContext context, DdtdExampleTaskDO data) {

            try {
                if (data == null) {
                    return TaskWorkState.PASS;
                }
                data.setStartAt(new Date());
                TimeUnit.SECONDS.sleep(5L);
                return TaskWorkState.SUCCESS;
            } catch (InterruptedException e) {
                log.info("任务中断。。。");
                return TaskWorkState.FAIL;
            }
        }

        @Override
        public TaskWorkState cleanupWork(ITaskContext context) {
            try {
                TimeUnit.SECONDS.sleep(5L);
                if (onCompleted != null) {
                    onCompleted.accept((TestTaskContext) context);
                }
                return TaskWorkState.SUCCESS;
            } catch (InterruptedException e) {
                log.info("清理中断。。。");
                return TaskWorkState.FAIL;
            }
        }

        @Override
        public String getVersion() {
            return "1";
        }

        @Override
        public String getName() {
            return "TestWorker2";
        }

        @Override
        public String getDescription() {
            return "测试分布式任务2";
        }
    }
}
