# celery-java
Java implementation of Celery client and worker

The aim is to be compatible with existing Python Celery implementation. That means you should be able
to run a Java client with a Python worker or vice-versa. Tested with Python Celery 4.1.

At the moment, this is a very alpha version. It can 

- execute a task
- report result
- report failure (and throw it as exception on the client)

What's missing:

- tests
- advanced features of Celery protocol
    - retries
    - chords
    - groups
    - chains
- published Javadoc
- (rather trivial) use a different RabbitMQ host than `localhost`

Patches providing any of these are welcome.

## Calling a Java task from Python

Implement the Task interface and register your tasks in META-INF as services. The easiest way is to annotate them
with [org.koshuke.MetaInfServices](http://metainf-services.kohsuke.org/).

We expect there to be a special method named `run` that will be used to handle incoming tasks. There may be multiple 
`run ` methods differing by their arguments as usually.

```java
package org.sedlakovi.celery.examples;

import org.kohsuke.MetaInfServices;
import org.sedlakovi.celery.Task;

@MetaInfServices
public class TestTask implements Task {

    public int run(int x, int y) {
        return x + y;
    }
}
```

Then, run Worker with this task on classpath. You can use the `examples` module

    $ mvn install
    $ cd examples && mv exec:java -Dexec.mainClass=org.sedlakovi.celery.examples.WorkerWithTestTasks
    [...]
    Started consuming tasks from queue celery.
    Known tasks:
      - org.sedlakovi.celery.examples.TestTask
      - org.sedlakovi.celery.examples.TestVoidTask

From the Python side, call the task by the class name.

```
In [1]: import celery

In [2]: app = celery.Celery(backend="rpc://localhost")

In [3]: app.signature("org.sedlakovi.celery.examples.TestTask", [1, 2]).delay().get()
Out[3]: 3

In [4]: %%timeit
   ...: app.signature("org.sedlakovi.celery.examples.TestTask", [1, 2]).delay().get()
   ...: 
2.1 ms ± 170 µs per loop (mean ± std. dev. of 7 runs, 100 loops each)
```

## Calling Python task from Java

1. Start a celery worker as described in [First Steps with Celery](http://docs.celeryproject.org/en/latest/getting-started/first-steps-with-celery.html).

2. Call the task by name.

```java
ConnectionFactory factory = new ConnectionFactory();
factory.setHost("localhost");
Connection connection = factory.newConnection(Executors.newCachedThreadPool());

RabbitBackend backend = new RabbitBackend(connection.createChannel());
Client client = new Client(connection.createChannel(), backend);

System.out.println(client.submit("tasks.add", 1, 2));
```

## Example code Java - RabbitMQ - Java

First, compile and install the Maven artifacts.

    mvn install

Then run the `examples` module. Make sure **RabbitMQ runs on localhost** and on standard port.

    cd examples && mvn exec:java -Dexec.mainClass=org.sedlakovi.celery.examples.Main

You should see output similar to this (redacted).

    [...]

    INFO: Task org.sedlakovi.celery.examples.TestTask[c89ff7fe-bd19-4d00-90b2-2739a66ab915] succeeded in 185.6 μs. Result was: 18
    Task #17's result was: 18. The task took 2.190 ms end-to-end.

    [...]

    I'm the task that just prints: 3
    INFO: Task org.sedlakovi.celery.examples.TestVoidTask[b51c7131-66ae-44c0-8e29-f19ab92af8a2] succeeded in 388.1 μs. Result was: null
    Testing result of void task: null

    [...]

    Testing task that should fail and throw exception:
    SEVERE: Task 049a0ff1-eea9-4744-9911-099c8deeddb7 dispatch error
    [WARNING]
    java.lang.reflect.InvocationTargetException
    [...]
    Caused by: org.sedlakovi.celery.WorkerException: DispatchException(Method run(class java.lang.String,class java.lang.String) not present in org.sedlakovi.celery.examples.TestTask)
    [...]
