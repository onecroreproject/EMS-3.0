package com.example.employee.dto;

public class DepartmentDTO {
    private String id;
    private String name;
    private String organizationName;

    public DepartmentDTO(String id, String name, String organizationName) {
        this.id = id;
        this.name = name;
        this.organizationName = organizationName;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getOrganizationName() { return organizationName; }
}

