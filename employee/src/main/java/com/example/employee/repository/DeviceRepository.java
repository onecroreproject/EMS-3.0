package com.example.employee.repository;

import com.example.employee.model.Device;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DeviceRepository extends MongoRepository<Device ,String> {

    Optional<Device> findByDeviceId(String deviceId);

    java.util.List<Device> findByEmployeeCode(String employeeCode);

    boolean existsByDeviceId(String deviceId);

}
