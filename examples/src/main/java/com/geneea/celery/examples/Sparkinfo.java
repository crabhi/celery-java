package com.geneea.celery.examples;

import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Created by stone on 18-3-5.
 */
public class Sparkinfo {
    private String numberExecutor;

    private String application;

    private String queue;

    private String deployMode;

    private String class_;

    private String executorCores;

    private String executorMemory;

    private LinkedList<String> args;

    private LinkedHashMap<String, String> kvargs;

    public Sparkinfo(String taskName) {
        args = new LinkedList<String>();
        kvargs = new LinkedHashMap<String, String>();
    }

    public String getNumberExecutor() {
        return numberExecutor;
    }

    public void setNumberExecutor(String numberExecutor) {
        this.numberExecutor = numberExecutor;
    }



    public String getApplication(){
        return this.application;
    }

    public void setApplication(String application) {
        this.application = application;
    }


    public LinkedList<String> getArgs() {
        return args;
    }

    public void setArgs(LinkedList<String> args) {
        this.args = args;
    }

    public LinkedHashMap<String, String> getKvargs() {
        return kvargs;
    }

    public void setKvargs(LinkedHashMap<String, String> kvargs) {
        this.kvargs = kvargs;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getDeployMode() {
        return deployMode;
    }

    public void setDeployMode(String deployMode) {
        this.deployMode = deployMode;
    }

    public String getClass_() {
        return class_;
    }

    public void setClass_(String class_) {
        this.class_ = class_;
    }

    public String getExecutorCores() {
        return executorCores;
    }

    public void setExecutorCores(String executorCores) {
        this.executorCores = executorCores;
    }

    public String getExecutorMemory() {
        return executorMemory;
    }

    public void setExecutorMemory(String executorMemory) {
        this.executorMemory = executorMemory;
    }
}
