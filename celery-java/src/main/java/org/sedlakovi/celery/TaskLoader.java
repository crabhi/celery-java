package org.sedlakovi.celery;

/**
 * Marker interface automatically added to proxy classes created by the {@link Task} annotation.
 */
public interface TaskLoader {
    Object loadTask();
}
