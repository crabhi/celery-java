package org.sedlakovi.celery;

import java.lang.annotation.*;

/**
 * Marks your code as a Celery CeleryTask.
 * <p>
 * The annotation processor included in this package then generates two classes - {@code *Proxy} and {@code *Loader}.
 * The loader is
 * <p>
 * All parameters and return types must be JSON-serializable.
 * <p>
 * In order for the {@link CeleryWorker} to find your Tasks, you must register them as a service in {@code META-INF/services}.
 * An easy way to do it is to annotate your CeleryTask implementation with {@code org.kohsuke.MetaInfServices} annotation. See
 * example tasks in the examples module.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface CeleryTask {
}
