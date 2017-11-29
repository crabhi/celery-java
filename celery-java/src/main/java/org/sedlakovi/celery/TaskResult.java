package org.sedlakovi.celery;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO representing result on the wire.
 */
class TaskResult {
    public List<?> children;
    public Status status;
    public Object result;
    public Object traceback;
    @JsonProperty("task_id") public String taskId;

    enum Status {
        SUCCESS,
        FAILURE,
    }
}
