package org.example.attendance;

public class LifecycleTest {

    public static void main(String[] args) {
        System.out.println("Starting Lifecycle Test...");
        AgentStateManager manager = new AgentStateManager();

        // Simulate new login
        manager.initialize("E-123", "EMP001", "DEV-123");
        manager.resetSessionForNewIdentity();

        // 1. LOGIN -> WORKING
        System.out.println("Transition: LOGIN -> WORKING");
        manager.startWork();
        assertState(manager, "ACTIVE", "WORKING");

        // 2. WORKING -> IDLE
        System.out.println("Transition: WORKING -> IDLE");
        manager.startIdle();
        assertState(manager, "ACTIVE", "IDLE");

        // 3. IDLE -> WORKING
        System.out.println("Transition: IDLE -> WORKING");
        manager.endIdle();
        assertState(manager, "ACTIVE", "WORKING");

        // 4. WORKING -> BREAK
        System.out.println("Transition: WORKING -> BREAK");
        manager.startBreak();
        assertState(manager, "ACTIVE", "BREAK");

        // 5. BREAK -> WORKING
        System.out.println("Transition: BREAK -> WORKING");
        manager.endBreak();
        assertState(manager, "ACTIVE", "WORKING");

        // 6. WORKING -> LUNCH
        System.out.println("Transition: WORKING -> LUNCH");
        manager.startLunch();
        assertState(manager, "ACTIVE", "LUNCH");

        // 7. LUNCH -> WORKING
        System.out.println("Transition: LUNCH -> WORKING");
        manager.endLunch();
        assertState(manager, "ACTIVE", "WORKING");

        // 8. WORKING -> WORK_ENDED
        System.out.println("Transition: WORKING -> WORK_ENDED");
        manager.endWork();
        assertState(manager, "ENDED", "CLOCKED_OUT");

        // Test Recovery Path
        System.out.println("Testing Crash/Restart -> RECOVERY_REQUIRED path...");
        
        // Setup crashed session
        manager.startWork(); // Session becomes ACTIVE
        
        // Simulate Crash/Restart by creating a new manager instance
        AgentStateManager newManager = new AgentStateManager();
        if (newManager.hasUnfinishedSession()) {
            System.out.println("Unfinished session detected correctly.");
            newManager.markRecoveryRequired();
            
            // Reload again to simulate seeing RECOVERY_REQUIRED
            AgentStateManager finalManager = new AgentStateManager();
            if (finalManager.hasUnfinishedSession() && "RECOVERY_REQUIRED".equals(finalManager.getState().getSessionStatus())) {
                System.out.println("RECOVERY_REQUIRED path validated successfully!");
            } else {
                throw new RuntimeException("Failed to validate RECOVERY_REQUIRED path. Status: " + finalManager.getState().getSessionStatus());
            }
        } else {
            throw new RuntimeException("Crash/Restart failed to detect unfinished session.");
        }

        System.out.println("All lifecycle tests passed successfully!");
    }

    private static void assertState(AgentStateManager manager, String expectedSessionStatus, String expectedCurrentState) {
        String sessionStatus = manager.getState().getSessionStatus();
        String currentState = manager.getState().getCurrentState();
        
        if (!expectedSessionStatus.equals(sessionStatus)) {
            throw new RuntimeException("Expected session status " + expectedSessionStatus + " but got " + sessionStatus);
        }
        if (!expectedCurrentState.equals(currentState)) {
            throw new RuntimeException("Expected current state " + expectedCurrentState + " but got " + currentState);
        }
        System.out.println(" -> Validated Session: " + sessionStatus + ", State: " + currentState);
    }
}
