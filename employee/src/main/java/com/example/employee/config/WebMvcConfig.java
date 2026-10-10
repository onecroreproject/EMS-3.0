package com.example.employee.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("login");
        registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("/dashboard").setViewName("admin-home");
        registry.addViewController("/admin/home").setViewName("admin-home");
        registry.addViewController("/index").setViewName("index");

        // Organization
        registry.addViewController("/organizations/new").setViewName("organization_form");
        registry.addViewController("/organizations/list").setViewName("organization_list");

        // Department
        registry.addViewController("/departments/new").setViewName("department_form");
        registry.addViewController("/departments/list").setViewName("department_list");

        // Team
        registry.addViewController("/teams/new").setViewName("team_form");
        registry.addViewController("/teams/list").setViewName("team_list");

        // Employee routes are handled by EmployeeWebController (@Controller)
        // which populates the model with ${employee}, ${departments}, ${employees}.
        // Do NOT add static view controllers here for /employees/* routes.
        
        // Tasks
        registry.addViewController("/tasks/new").setViewName("task-form");
        registry.addViewController("/tasks/list").setViewName("task-list");
        registry.addViewController("/tasks").setViewName("task-list");

        // Admin other pages
        registry.addViewController("/admin/time-tracking/summary").setViewName("employee-time-summary");
        registry.addViewController("/admin/activity-logs").setViewName("activity-logs");
        registry.addViewController("/admin/attendance").setViewName("attendance-logs");
        registry.addViewController("/admin/productivity").setViewName("admin-productivity");
        registry.addViewController("/admin/locations").setViewName("admin-locations");
    }
}
