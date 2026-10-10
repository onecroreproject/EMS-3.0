package com.example.employee.service;

import com.example.employee.model.Device;
import com.example.employee.repository.DeviceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class DeviceStatusService {

    private final DeviceRepository deviceRepository;

    // Development threshold: 30 seconds
    private static final long OFFLINE_THRESHOLD_SECONDS = 30;

    public DeviceStatusService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Scheduled(fixedRate = 10_000)
    public void updateDeviceStatus() {

        Instant now = Instant.now();

        for (Device device : deviceRepository.findAll()) {

            if (device.getLastSeen() == null) {
                device.setStatus("OFFLINE");
            } else {

                long secondsSinceLastSeen =
                        Duration.between(
                                device.getLastSeen(),
                                now
                        ).getSeconds();

                if (secondsSinceLastSeen >
                        OFFLINE_THRESHOLD_SECONDS) {

                    device.setStatus("OFFLINE");

                } else {

                    device.setStatus("ONLINE");
                }
            }

            deviceRepository.save(device);
        }
    }
}
