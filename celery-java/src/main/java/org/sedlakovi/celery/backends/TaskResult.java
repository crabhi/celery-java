package org.sedlakovi.celery.backends;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TaskResult {
    public List<?> children;
    public Status status;
    public Object result;
    public Object traceback;
    @JsonProperty("task_id") public String taskId;

    public enum Status {
        SUCCESS,
        FAILURE,
    }
}
