package org.sedlakovi.celery;

import java.lang.annotation.*;

/**
 * Marker interface for the Celery Task.
 * <p>
 * It must have a method named {@code run} with the parameters of the called task. If such method is not found,
 * a {@link DispatchException} is thrown at runtime and reported back to the client.
 * <p>
 * All parameters and return types must be JSON-serializable.
 * <p>
 * In order for the {@link Worker} to find your Tasks, you must register them as a service in {@code META-INF/services}.
 * An easy way to do it is to annotate your Task implementation with {@code org.kohsuke.MetaInfServices} annotation. See
 * example tasks in the examples module.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Task {
}
