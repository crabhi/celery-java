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

Patches providing any of these are welcome.

## Javadoc

Check out generated Javadoc at [http://crabhi.github.io/celery-java/apidocs/](http://crabhi.github.io/celery-java/apidocs/).

## Calling a Java task from Python

1. Annotate your class that does something useful as a `@Task`.

    ```java
    import org.sedlakovi.celery.Task;
    
    @Task
    public class TestTask {
    
        public int sum(int x, int y) {
            return x + y;
        }
    }
    ```

2. Run `Worker` with your tasks on classpath. You can directly use the `Worker` class or embed it into your `main` 
function.

    ```java
    import org.sedlakovi.celery.Worker;
    
    public class MyWorker {
        public static void main(String[] args) throws Exception {
            Worker.main(args);
        }
    }
    ```

3. From the Python side, call the task by the class name hash (`#`) method name.

    ```python
    In [1]: import celery
    
    In [2]: app = celery.Celery(broker="amqp://localhost/", backend="rpc://localhost")
    
    In [3]: app.signature("org.sedlakovi.celery.examples.TestTask#sum", [1, 2]).delay().get()
    Out[3]: 3
    
    In [4]: %%timeit
       ...: app.signature("org.sedlakovi.celery.examples.TestTask#sum", [1, 2]).delay().get()
       ...: 
    2.1 ms ± 170 µs per loop (mean ± std. dev. of 7 runs, 100 loops each)
    ```

## Calling Python task from Java

1. Start a celery worker as described in [First Steps with Celery][celery-py-start].

2. Call the task by name.

```java
RabbitBackend backend = new RabbitBackend(rabbitConnection.createChannel());
Client client = new Client(rabbitConnection.createChannel(), backend);

System.out.println(client.submit("tasks.add", 1, 2).get());
```

## Calling Java task from Java

The `@Task` annotation on a class `X` causes `XProxy` and `XLoader` to be generated. `XLoader` registers the task into 
the worker and `XProxy` has all the task methods tweaked so they now return a `Future<...>` instead of the original
type.

To use the proxy, you need a Celery `Client`.

```java
Client client = new Client(rabbitConnectionChannel, rabbitBackend);

Integer result = TestTaskProxy.with(client).sum(1, 7).get();
```

[celery-py-start]: http://docs.celeryproject.org/en/latest/getting-started/first-steps-with-celery.html