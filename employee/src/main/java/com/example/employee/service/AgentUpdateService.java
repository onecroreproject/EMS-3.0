package com.example.employee.service;

import com.example.employee.model.AgentUpdate;
import com.example.employee.repository.AgentUpdateRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentUpdateService {

    private final AgentUpdateRepository repository;

    public AgentUpdateService(
            AgentUpdateRepository repository) {

        this.repository = repository;
    }

    /**
     * Finds the latest active update for the requested platform.
     *
     * Version comparison is performed in Java
     * so that versions such as 1.0.10 are handled
     * correctly.
     */
    public AgentUpdate getLatestUpdate(
            String currentVersion,
            String platform) {

        List<AgentUpdate> updates =
                repository.findByPlatformAndActiveTrue(
                        platform
                );

        if (updates.isEmpty()) {
            return null;
        }

        AgentUpdate latest = null;

        for (AgentUpdate update : updates) {

            if (latest == null) {
                latest = update;
                continue;
            }

            if (compareVersions(
                    latest.getVersion(),
                    update.getVersion()) < 0) {

                latest = update;
            }
        }

        if (latest == null) {
            return null;
        }

        /*
         * Current agent is already on the same
         * or newer version.
         */
        if (compareVersions(
                currentVersion,
                latest.getVersion()) >= 0) {

            return null;
        }

        return latest;
    }

    /**
     * Saves a new Agent update release.
     */
    public AgentUpdate saveUpdate(
            AgentUpdate agentUpdate) {

        return repository.save(agentUpdate);
    }

    /**
     * Finds a specific release.
     */
    public AgentUpdate findByVersion(
            String version,
            String platform) {

        return repository
                .findByVersionAndPlatform(
                        version,
                        platform
                )
                .orElse(null);
    }

    /**
     * Deactivates all existing active releases
     * for the specified platform.
     *
     * The newly published release can then become
     * the only active release.
     */
    public void deactivateActiveUpdates(
            String platform) {

        List<AgentUpdate> activeUpdates =
                repository.findByPlatformAndActiveTrue(
                        platform
                );

        for (AgentUpdate update : activeUpdates) {

            update.setActive(false);

            repository.save(update);
        }
    }

    /**
     * Compares semantic versions.
     *
     * Examples:
     *
     * 1.0.0 < 1.0.1
     * 1.0.9 < 1.0.10
     * 1.9.0 < 1.10.0
     */
    private int compareVersions(
            String currentVersion,
            String latestVersion) {

        if (currentVersion == null
                || latestVersion == null) {

            throw new IllegalArgumentException(
                    "Version cannot be null"
            );
        }

        String[] current =
                currentVersion
                        .trim()
                        .split("\\.");

        String[] latest =
                latestVersion
                        .trim()
                        .split("\\.");

        int length =
                Math.max(
                        current.length,
                        latest.length
                );

        for (int i = 0; i < length; i++) {

            int currentPart =
                    i < current.length
                            ? Integer.parseInt(current[i])
                            : 0;

            int latestPart =
                    i < latest.length
                            ? Integer.parseInt(latest[i])
                            : 0;

            if (currentPart < latestPart) {
                return -1;
            }

            if (currentPart > latestPart) {
                return 1;
            }
        }

        return 0;
    }
}
