# celery-java
Java implementation of Celery client and worker

## Running

First, compile and install the Maven artifacts.

    mvn install

Then run the `examples` module. Make sure **RabbitMQ runs on localhost** and on standard port.

    cd examples && mvn exec:java

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
