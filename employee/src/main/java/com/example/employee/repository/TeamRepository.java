package com.example.employee.repository;

import com.example.employee.model.Team;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends MongoRepository<Team, String> {
  List<Team> findByDepartmentId(String departmentId);


}
