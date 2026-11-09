package com.cloudclean.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class StorageConnectionRequest {

    @NotBlank(message = "Connection name is required")
    private String name;

    private List<String> paths;

    public StorageConnectionRequest() {}

    public StorageConnectionRequest(String name, List<String> paths) {
        this.name = name;
        this.paths = paths;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }
}
