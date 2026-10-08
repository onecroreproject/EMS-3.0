package com.example.employee.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employee.model.Employee;

public interface EmployeeRepository extends MongoRepository<Employee, String> {

     Optional<Employee> findByEmployeeCode(String employeeCode);

      Optional<Employee> findById(String id);
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByPhone(String phone);

    // OR write custom method for combined search
   Optional<Employee> findByIdOrEmailOrPhoneOrEmployeeCode(String id, String email, String phone, String employeeCode);

  Optional<Employee> findByName(String name);

    List<Employee> findByTeamId(String teamId);


    List<Employee> findTop5ByOrderByPointsDesc(); // Or Top1, Top3, etc.

  

}
