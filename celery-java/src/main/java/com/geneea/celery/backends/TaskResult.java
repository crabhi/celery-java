package com.geneea.celery.backends;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO representing result on the wire.
 */
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
