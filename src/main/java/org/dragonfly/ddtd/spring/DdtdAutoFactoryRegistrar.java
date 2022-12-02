package org.dragonfly.ddtd.spring;

import com.google.common.collect.Maps;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.MapUtils;
import org.dragonfly.ddtd.conf.GlobalProperties;
import org.dragonfly.ddtd.framework.TaskZooKeeperFactory;
import org.dragonfly.ddtd.framework.inspect.ITaskInspect;
import org.dragonfly.ddtd.framework.report.ITaskReporter;
import org.dragonfly.ddtd.framework.task.DefaultTaskFactory;
import org.dragonfly.ddtd.framework.task.ITaskWorker;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * Auto detect any factories of dynamic distributed task dispatcher.
 *
 * @author jian.dong1
 * @version 1.0
 * @date 2022/11/28
 **/
@SuppressWarnings({"unchecked", "rawtypes"})
public class DdtdAutoFactoryRegistrar implements InitializingBean, DisposableBean {

    /**
     * Key indicate the implemented class of task worker, the key of value map indicate name of task work bean.
     */
    private Map<Class<ITaskWorker>, Map<String, DefaultTaskFactory<?>>> factories;

    private final ApplicationContext applicationContext;

    private final GlobalProperties globalProperties;

    private final TaskZooKeeperFactory taskZooKeeperFactory;

    private final ITaskReporter taskReporter;

    private final ITaskInspect taskInspect;

    public DdtdAutoFactoryRegistrar(ApplicationContext applicationContext,
                                    GlobalProperties globalProperties,
                                    TaskZooKeeperFactory taskZooKeeperFactory,
                                    ITaskReporter taskReporter,
                                    ITaskInspect taskInspect) {
        this.applicationContext = applicationContext;
        this.globalProperties = globalProperties;
        this.taskZooKeeperFactory = taskZooKeeperFactory;
        this.taskReporter = taskReporter;
        this.taskInspect = taskInspect;
    }

    @Override
    public void destroy() throws Exception {

        if (MapUtils.isNotEmpty(factories)) {
            for (Map<String, DefaultTaskFactory<?>> innerFactories : factories.values()) {
                for (DefaultTaskFactory<?> factory : innerFactories.values()) {
                    factory.destroy();
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        final Map<String, ITaskWorker> beansOfType = applicationContext.getBeansOfType(ITaskWorker.class);
        if (MapUtils.isEmpty(beansOfType)) {
            return;
        }
        factories = Maps.newHashMap();
        for (Map.Entry<String, ITaskWorker> taskWorker : beansOfType.entrySet()) {

            final Class<ITaskWorker> existsKey = (Class<ITaskWorker>) taskWorker.getValue().getClass();
            final Map<String, DefaultTaskFactory<?>> existsFactories = factories.computeIfAbsent(existsKey, (key) -> Maps.newHashMap());
            existsFactories.put(taskWorker.getKey(), newFactory(taskWorker.getValue()));
        }
    }

    /**
     * Get the factory instance of dynamic distributed task dispatcher
     *
     * @param taskWorker the class of implement for {@link ITaskWorker}, must managed by Spring
     * @return task factory if exists or null.
     */
    public DefaultTaskFactory<?> getFactory(Class<? extends ITaskWorker> taskWorker) {

        if (MapUtils.isEmpty(factories)) {
            return null;
        }
        try {

            String factoryName;
            final Map<String, ? extends ITaskWorker> beansManaged =
                    applicationContext.getBeansOfType(taskWorker);
            if (MapUtils.isEmpty(beansManaged)) {
                throw new IllegalArgumentException("No factory name can be find, do you forget managed by Spring?");
            } else if (beansManaged.size() > 1) {
                throw new IllegalArgumentException("Find multiple task workers, please give us a name explicitly");
            } else {
                factoryName = IteratorUtils.get(beansManaged.keySet().iterator(), 0);
            }
            final Map<String, DefaultTaskFactory<?>> existsFactories =
                    factories.computeIfAbsent((Class<ITaskWorker>)taskWorker, (key) -> Maps.newHashMap());
            return existsFactories.get(factoryName);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * Get the factory instance of dynamic distributed task dispatcher
     *
     * @param factoryName name to find it
     * @param taskWorker the class of implement for {@link ITaskWorker}
     * @param <T>        the task data type
     * @return task factory if exists or null.
     */
    public <T> DefaultTaskFactory<T> getFactory(String factoryName, Class<? extends ITaskWorker> taskWorker) {
        if (MapUtils.isEmpty(factories)) {
            return null;
        }
        try {
            final Map<String, DefaultTaskFactory<?>> existsFactories =
                    factories.computeIfAbsent((Class<ITaskWorker>) taskWorker, (key) -> Maps.newHashMap());
            return (DefaultTaskFactory<T>) existsFactories.get(factoryName);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * build new one ddtd factory and register for using it.
     * @param factoryName name to find it
     * @param taskWorker task worker instance
     * @param <T> type of worker
     */
    public <T> void register(String factoryName, ITaskWorker<T> taskWorker) {
        final Class<ITaskWorker> workerClass = (Class<ITaskWorker>)taskWorker.getClass();
        final Map<String, DefaultTaskFactory<?>> existsFactories = factories.computeIfAbsent(workerClass, (key) -> Maps.newHashMap());
        if (existsFactories.containsKey(factoryName)) {
            throw new IllegalArgumentException("Duplicated name of task worker: " + taskWorker.getName());
        }
        existsFactories.put(factoryName, newFactory(taskWorker));
    }

    /**
     * build new one ddtd factory and register for using it.
     * <p>
     *     use worker's name to named factory
     * </p>
     * @param taskWorker task worker instance
     * @param <T> type of worker
     */
    public <T> void register(ITaskWorker<T> taskWorker) {
        this.register(taskWorker.getName(), taskWorker);
    }

    private DefaultTaskFactory newFactory(ITaskWorker taskWorker) {
        return new DefaultTaskFactory<>(
                globalProperties,
                taskZooKeeperFactory,
                taskWorker,
                taskInspect,
                taskReporter
        );
    }
}
