# celery-java

Java implementation of [Celery][celery] client and worker. Quoting from the project website:

> Celery is an asynchronous task queue/job queue based on distributed message passing. It is focused on real-time operation, but supports scheduling as well.

>  The execution units, called tasks, are executed concurrently on a single or more worker servers using multiprocessing, Eventlet, or gevent. Tasks can execute asynchronously (in the background) or synchronously (wait until ready).

>  Celery is used in production systems to process millions of tasks a day.

The aim is to be compatible with existing [Python Celery implementation][celery]. That means you should be able
to run a Java client with a Python worker or vice-versa. Tested with Python Celery 4.1.

At the moment, this is a very alpha version. It can

- execute a task
- report result
- report failure (and throw it as exception on the client)

What's missing:

- advanced features of Celery protocol
    - retries
    - chords
    - groups
    - chains

Patches providing any of these are welcome.

## Maven dependency

Releases are available from Maven Central. Latest version: [![Maven
Central](https://maven-badges.herokuapp.com/maven-central/com.geneea.celery/celery-java/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.geneea.celery%22%20AND%20a%3A%22celery-java%22)

```xml
<dependency>
    <groupId>org.sedlakovi.celery</groupId>
    <artifactId>celery-java</artifactId>
    <version>...</version>
</dependency>
```

Snapshots are available from [Sonatype OSRH](https://oss.sonatype.org/content/groups/public):

```xml
<repository>
    <id>sonatype</id>
    <url>https://oss.sonatype.org/content/groups/public</url>
    <snapshots>
        <enabled>true</enabled>
        <updatePolicy>always</updatePolicy>
    </snapshots>
</repository>
```

## Javadoc

Check out generated Javadoc at [http://crabhi.github.io/celery-java/apidocs/](http://crabhi.github.io/celery-java/apidocs/).

## Calling a Java task from Python

1. Annotate your class that does something useful as a `@CeleryTask`.

    ```java
    import com.geneea.celery.CeleryTask;

    @CeleryTask
    public class TestTask {

        public int sum(int x, int y) {
            return x + y;
        }
    }
    ```

2. Run `Worker` with your tasks on classpath. You can directly use the `Worker` class or embed it into your `main`
function.

    ```java
    import com.geneea.celery.CeleryWorker;

    public class MyWorker {
        public static void main(String[] args) throws Exception {
            CeleryWorker.main(args);
        }
    }
    ```

3. From the Python side, call the task by the class name hash (`#`) method name.

    ```python
    In [1]: import celery

    In [2]: app = celery.Celery(broker="amqp://localhost/", backend="rpc://localhost")

    In [3]: app.signature("com.geneea.celery.examples.TestTask#sum", [1, 2]).delay().get()
    Out[3]: 3

    In [4]: %%timeit
       ...: app.signature("com.geneea.celery.examples.TestTask#sum", [1, 2]).delay().get()
       ...:
    2.1 ms ± 170 µs per loop (mean ± std. dev. of 7 runs, 100 loops each)
    ```

## Calling Python task from Java

1. Start a celery worker as described in [First Steps with Celery][celery-py-start].

2. Call the task by name.

```java
Celery client = Celery.builder()
        .brokerUri("amqp://localhost/%2F")
        .backendUri("rpc://localhost/%2F")
        .build();

System.out.println(client.submit("tasks.add", new Object[]{1, 2}).get());
```

## Calling Java task from Java

The `@CeleryTask` annotation on a class `MyClass` causes `MyClassProxy` and `MyClassLoader` to be generated.
`MyClassLoader` registers the task into the worker and `MyClassProxy` has all the task methods tweaked so they
now return a `Future<...>` instead of the original type.

To use the proxy, you need a Celery `Client`.

```java
Celery client = Celery.builder()
        .brokerUri("amqp://localhost/%2F")
        .backendUri("rpc://localhost/%2F")
        .build();

Integer result = TestTaskProxy.with(client).sum(1, 7).get();
```

## Development

### Local build

Build with `mvn -Dgpg.skip` to avoid the signing step.

### Releasing

    mvn release:clean release:prepare
    mvn release:perform

### Tests

Unit tests are part of the `celery-java` module. Integration tests are part of the `examples` module and are
based on the example tasks.
They start the queue in backend automatically via Docker. You need to have Docker configured on the machine running
the tests of the `examples` module.

## Relase notes

* 1.2 - Moved the package from `org.sedlakovi` to `com.geneea`. No functionality changes.
* 1.1 - Lots of breaking changes (renames, API improvements, the Client can be constructed
  without an existing connection...). If you're already using this library, please get in
  touch. The next release should have much fewer breaking changes and they will be listed
  explicitly.

* 1.0 - Initial release. Don't expect it to be stable.


[celery-py-start]: http://docs.celeryproject.org/en/latest/getting-started/first-steps-with-celery.html
[celery]: http://www.celeryproject.org/
