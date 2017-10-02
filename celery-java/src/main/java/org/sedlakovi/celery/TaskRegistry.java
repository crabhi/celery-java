package org.sedlakovi.celery;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;

/**
 * Loads registered {@link Task} services and provides them by their class name.
 */
class TaskRegistry {

    private static final Map<String, Task> TASKS = Streams
            .stream(ServiceLoader.load(Task.class))
            .collect(ImmutableMap.toImmutableMap((v) -> v.getClass().getName(), Function.identity()));

    static Set<String> getRegisteredTaskNames() {
        return TASKS.keySet();
    }

    static Task getTask(String taskName) {
        return TASKS.get(taskName);
    }
}
