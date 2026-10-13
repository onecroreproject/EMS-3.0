package com.example.employee.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.employee.model.Employee;

public interface EmployeeRepository extends MongoRepository<Employee, String> {

     Optional<Employee> findFirstByEmployeeCode(String employeeCode);

      Optional<Employee> findFirstById(String id);
    Optional<Employee> findFirstByEmail(String email);
    Optional<Employee> findFirstByPhone(String phone);

    // OR write custom method for combined search
   Optional<Employee> findFirstByIdOrEmailOrPhoneOrEmployeeCode(String id, String email, String phone, String employeeCode);

  Optional<Employee> findFirstByName(String name);

    List<Employee> findByTeamId(String teamId);


    List<Employee> findTop5ByOrderByPointsDesc(); // Or Top1, Top3, etc.

  

}
