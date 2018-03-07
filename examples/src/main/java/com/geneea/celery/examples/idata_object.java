package com.geneea.celery.examples;

/**
 * Created by stone on 18-2-8.
 */
public class idata_object {
    private String _cron = "";
    private String _principal = "";
    private int _num_executors = 1;
    private String _dag_id = "";
    private String _start_date = "";

    public String get_cron() {
        return _cron;
    }

    public void set_cron(String _cron) {
        this._cron = _cron;
    }

    public int get_num_executors() {
        return _num_executors;
    }

    public void set_num_executors(int _num_executors) {
        this._num_executors = _num_executors;
    }

    public String get_dag_id() {
        return _dag_id;
    }

    public void set_dag_id(String _dag_id) {
        this._dag_id = _dag_id;
    }

    public String get_start_date() {
        return _start_date;
    }

    public void set_start_date(String _start_date) {
        this._start_date = _start_date;
    }

    public String get_principal() {
        return _principal;
    }

    public void set_principal(String _principal) {
        this._principal = _principal;
    }
}
