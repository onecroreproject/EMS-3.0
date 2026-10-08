package com.example.employee.controller.api.admin;

import com.example.employee.model.Team;
import com.example.employee.repository.TeamRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/teams")
public class AdminTeamController {

    private final TeamRepository teamRepository;

    public AdminTeamController(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @GetMapping
    public ResponseEntity<List<Team>> getAllTeams() {
        return ResponseEntity.ok(teamRepository.findAll());
    }

    @GetMapping("/by-dept/{deptId}")
    public ResponseEntity<List<Team>> getTeamsByDepartment(@PathVariable String deptId) {
        return ResponseEntity.ok(teamRepository.findByDepartmentId(deptId));
    }

    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestBody Team team) {
        return ResponseEntity.ok(teamRepository.save(team));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Team> updateTeam(@PathVariable String id, @RequestBody Team teamDetails) {
        Optional<Team> opt = teamRepository.findById(id);
        if (opt.isPresent()) {
            Team team = opt.get();
            team.setName(teamDetails.getName());
            team.setDepartmentId(teamDetails.getDepartmentId());
            return ResponseEntity.ok(teamRepository.save(team));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable String id) {
        teamRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
