package org.sedlakovi.celery;

/**
 * Service (instantiated by {@link java.util.ServiceLoader}) instantiating a {@link Task} within {@link Worker}.
 * <p>
 * Such loader is generated automatically when processing the {@link Task} annotation.
 */
public interface TaskLoader {
    Object loadTask();
}
