package com.example.employee.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.employee.dto.TeamDTO;
import com.example.employee.model.Department;
import com.example.employee.model.Team;
import com.example.employee.repository.DepartmentRepository;
import com.example.employee.repository.TeamRepository;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    public Team save(Team team) {
        return teamRepository.save(team);
    }

    public List<Team> getAll() {
        return teamRepository.findAll();
    }

    public Optional<Team> getById(String id) {
        return teamRepository.findById(id);
    }

    public void deleteById(String id) {
        teamRepository.deleteById(id);
    }

   public List<TeamDTO> getTeamsByDepartment(String departmentId) {
    List<Team> teams = teamRepository.findByDepartmentId(departmentId);

    final String departmentName = departmentRepository.findById(departmentId)
            .map(Department::getName)
            .orElse("");

    return teams.stream()
            .map(t -> new TeamDTO(t.getId(), t.getName(), departmentName))
            .collect(Collectors.toList());
}

   
}
