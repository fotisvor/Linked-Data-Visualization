package com.example.demo;

import java.util.List;

public class State {
    private String name;
    private List<String> counties;

    public String getName() {
        return name;
    }

    public List<String> getCounties() {
        return counties;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCounties(List<String> counties) {
        this.counties = counties;
    }
}

