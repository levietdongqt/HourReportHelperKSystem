package com.mytech.hourreporthelper;

import java.util.ArrayList;
import java.util.List;

public class ReportResult {
    private String id; // email address (username)
    private List<String> result; // list of uploaded file names

    public ReportResult() {
        this.result = new ArrayList<>();
    }

    public ReportResult(String id) {
        this.id = id;
        this.result = new ArrayList<>();
    }

    public ReportResult(String id, List<String> result) {
        this.id = id;
        this.result = result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getResult() {
        return result;
    }

    public void setResult(List<String> result) {
        this.result = result;
    }

    public void addResult(String fileName) {
        if (this.result == null) {
            this.result = new ArrayList<>();
        }
        this.result.add(fileName);
    }
} 