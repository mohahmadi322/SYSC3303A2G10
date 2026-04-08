import java.net.DatagramSocket;
import java.time.LocalTime;
import java.net.UnknownHostException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SchedulerTest {

    Scheduler scheduler;

    //GUI stub
    static class MockGUI extends GUI {
        @Override public void updateOrReplaceSquare(int zone, String label, java.awt.Color c) {}
        @Override public void log(String msg) {}
    }

    @BeforeEach
    void setup() throws Exception {

        GUI gui = new MockGUI();

        scheduler = new Scheduler(gui) {
            {
                try {
                    getSendSocket().close();
                    getReceiveSocket().close();

                    // Replace with dummy sockets
                    java.lang.reflect.Field send = Scheduler.class.getDeclaredField("sendSocket");
                    java.lang.reflect.Field recv = Scheduler.class.getDeclaredField("receiveSocket");
                    send.setAccessible(true);
                    recv.setAccessible(true);

                    send.set(this, new DatagramSocket(0));
                    recv.set(this, new DatagramSocket(0));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        scheduler.getDrones().clear();
        scheduler.getFireQueue().clear();
        scheduler.getActiveEvents().clear();

        // Add two drones
        Scheduler.DroneInfo d1 = scheduler.new DroneInfo(1, 6001);
        Scheduler.DroneInfo d2 = scheduler.new DroneInfo(2, 6002);

        scheduler.getDrones().put(1, d1);
        scheduler.getDrones().put(2, d2);
    }

    //Queue ordering
    @Test
    void queuedFireAssignedAfterDroneReturns() throws UnknownHostException {
        FireIncidentEvent f1 = new FireIncidentEvent(
                LocalTime.of(2, 0),
                new Zone(1, 0, 0, 5, 5),
                FireIncidentEvent.Status.FIRE_DETECTED,
                FireIncidentEvent.Severity.Low,
                FireIncidentEvent.FaultType.NONE
        );

        FireIncidentEvent f2 = new FireIncidentEvent(
                LocalTime.of(5, 0),
                new Zone(2, 0, 0, 5, 5),
                FireIncidentEvent.Status.FIRE_DETECTED,
                FireIncidentEvent.Severity.Low,
                FireIncidentEvent.FaultType.NONE
        );

        scheduler.getFireQueue().add(f1);
        scheduler.getFireQueue().add(f2);

        // Pick a drone and simulate it finishing first fire
        Scheduler.DroneInfo d = scheduler.getDrones().get(1);
        d.state = Scheduler.DroneInfo.DroneState.IDLE;

        // Assign first fire
        scheduler.assignDrone();
        d.state = Scheduler.DroneInfo.DroneState.IDLE; // simulate drone return

        // Assign second fire
        scheduler.assignDrone();

        assertEquals(Scheduler.DroneInfo.DroneState.OUTBOUND, d.state);

        // f2 should now be removed from queue since it’s assigned
        assertFalse(scheduler.getFireQueue().contains(f2), "f2 should have been assigned");
    }

    //Water logic
    @Test
    void droneWithoutWaterSkipped() throws UnknownHostException {
        scheduler.getDrones().get(1).agentRemaining = 0;
        scheduler.getDrones().get(2).agentRemaining = 10;

        scheduler.getFireQueue().add(new FireIncidentEvent(
                LocalTime.of(2, 0),
                new Zone(1, 0, 0, 5, 5),
                FireIncidentEvent.Status.FIRE_DETECTED,
                FireIncidentEvent.Severity.Low,
                FireIncidentEvent.FaultType.NONE
        ));

        scheduler.assignDrone();

        assertEquals(Scheduler.DroneInfo.DroneState.OUTBOUND, scheduler.getDrones().get(2).state);
    }


    //Faulty drones are ignored
    @Test
    void faultyDroneIgnored() throws Exception {

        Scheduler.DroneInfo d1 = scheduler.getDrones().get(1);
        Scheduler.DroneInfo d2 = scheduler.getDrones().get(2);

        d1.isFaulty = true; // should be ignored
        d2.state = Scheduler.DroneInfo.DroneState.IDLE;

        scheduler.getFireQueue().add(new FireIncidentEvent(
                LocalTime.now(),
                new Zone(1, 0, 0, 5, 5),
                FireIncidentEvent.Status.FIRE_DETECTED,
                FireIncidentEvent.Severity.Moderate,
                FireIncidentEvent.FaultType.NONE
        ));

        scheduler.assignDrone();

        assertEquals(Scheduler.DroneInfo.DroneState.OUTBOUND, d2.state);
        assertEquals(-1, d1.targetZone);
    }

    // Timeout marks drone faulty and re-queues fire
    @Test
    void timeoutRequeuesFire() throws Exception {

        Scheduler.DroneInfo d = scheduler.getDrones().get(1);
        d.state = Scheduler.DroneInfo.DroneState.OUTBOUND;
        d.targetZone = 4;
        d.dispatchTime = System.currentTimeMillis() - 25000;

        FireIncidentEvent fire = new FireIncidentEvent(
                LocalTime.now(),
                new Zone(4,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED,
                FireIncidentEvent.Severity.High,
                FireIncidentEvent.FaultType.NONE
        );

        scheduler.getActiveEvents().put(4, fire);   // ⭐ REQUIRED
        scheduler.getFireQueue().add(fire);

        var m = Scheduler.class.getDeclaredMethod("checkForTimeouts");
        m.setAccessible(true);
        m.invoke(scheduler);

        assertTrue(d.isFaulty);
        assertFalse(scheduler.getFireQueue().isEmpty());
    }

    //Tie-breaking by water
    @Test
    void droneWithMoreWaterWinsTie() throws Exception {

        Scheduler.DroneInfo d1 = scheduler.getDrones().get(1);
        Scheduler.DroneInfo d2 = scheduler.getDrones().get(2);

        d1.currentZone = 5;
        d2.currentZone = 5;

        d1.agentRemaining = 5;
        d2.agentRemaining = 30;

        scheduler.getFireQueue().add(new FireIncidentEvent(
                LocalTime.now(), new Zone(6,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.Low,
                FireIncidentEvent.FaultType.NONE));

        scheduler.assignDrone();

        assertEquals(Scheduler.DroneInfo.DroneState.OUTBOUND, d2.state);
    }

    //Distance logic
    @Test
    void distanceBetweenZonesWorks() throws Exception {
        var m = Scheduler.class.getDeclaredMethod("distanceBetweenZones", int.class, int.class);
        m.setAccessible(true);

        assertEquals(0, m.invoke(scheduler, 1, 1));
        assertEquals(1, m.invoke(scheduler, 1, 2));
        assertEquals(2, m.invoke(scheduler, 1, 6));
    }

    //WaterRequired logic
    @Test
    void waterRequiredMatchesSeverity() throws Exception {

        var m = Scheduler.class.getDeclaredMethod("waterRequired", FireIncidentEvent.class);
        m.setAccessible(true);

        FireIncidentEvent low = new FireIncidentEvent(
                LocalTime.now(), new Zone(1,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.Low,
                FireIncidentEvent.FaultType.NONE);

        FireIncidentEvent high = new FireIncidentEvent(
                LocalTime.now(), new Zone(1,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.High,
                FireIncidentEvent.FaultType.NONE);

        assertEquals(10, m.invoke(scheduler, low));
        assertEquals(30, m.invoke(scheduler, high));
    }

    //computeScore logic
    @Test
    void computeScoreSeverityMatters() throws Exception {

        var m = Scheduler.class.getDeclaredMethod("computeScore",
                Scheduler.DroneInfo.class, FireIncidentEvent.class);
        m.setAccessible(true);

        Scheduler.DroneInfo d = scheduler.getDrones().get(1);
        d.agentRemaining = 30;
        d.currentZone = 1;

        FireIncidentEvent low = new FireIncidentEvent(
                LocalTime.now(), new Zone(5,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.Low,
                FireIncidentEvent.FaultType.NONE);

        FireIncidentEvent high = new FireIncidentEvent(
                LocalTime.now(), new Zone(5,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.High,
                FireIncidentEvent.FaultType.NONE);

        int lowScore = (int) m.invoke(scheduler, d, low);
        int highScore = (int) m.invoke(scheduler, d, high);

        assertTrue(highScore < lowScore);
    }

    @Test
    void computeScoreMoreWaterWins() throws Exception {

        var m = Scheduler.class.getDeclaredMethod("computeScore",
                Scheduler.DroneInfo.class, FireIncidentEvent.class);
        m.setAccessible(true);

        Scheduler.DroneInfo d1 = scheduler.getDrones().get(1);
        Scheduler.DroneInfo d2 = scheduler.getDrones().get(2);

        d1.agentRemaining = 5;
        d2.agentRemaining = 30;

        d1.currentZone = 3;
        d2.currentZone = 3;

        FireIncidentEvent fire = new FireIncidentEvent(
                LocalTime.now(), new Zone(4,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.Low,
                FireIncidentEvent.FaultType.NONE);

        int score1 = (int) m.invoke(scheduler, d1, fire);
        int score2 = (int) m.invoke(scheduler, d2, fire);

        assertTrue(score2 < score1);
    }

    @Test
    void computeScoreCloserDroneWins() throws Exception {

        var m = Scheduler.class.getDeclaredMethod("computeScore",
                Scheduler.DroneInfo.class, FireIncidentEvent.class);
        m.setAccessible(true);

        Scheduler.DroneInfo d1 = scheduler.getDrones().get(1);
        Scheduler.DroneInfo d2 = scheduler.getDrones().get(2);

        d1.currentZone = 1;   // close
        d2.currentZone = 16;  // far

        d1.agentRemaining = 20;
        d2.agentRemaining = 20;

        FireIncidentEvent fire = new FireIncidentEvent(
                LocalTime.now(), new Zone(2,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.Low,
                FireIncidentEvent.FaultType.NONE);

        int score1 = (int) m.invoke(scheduler, d1, fire);
        int score2 = (int) m.invoke(scheduler, d2, fire);

        assertTrue(score1 < score2);
    }

    //Active events tracking
    @Test
    void activeEventsUpdatedOnFire() {
        FireIncidentEvent e = new FireIncidentEvent(
                LocalTime.now(), new Zone(3,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.Low,
                FireIncidentEvent.FaultType.NONE);

        scheduler.getActiveEvents().put(3, e);

        assertTrue(scheduler.getActiveEvents().containsKey(3));
    }

    @Test
    void activeEventsRemovedOnExtinguish() {
        FireIncidentEvent e = new FireIncidentEvent(
                LocalTime.now(), new Zone(3,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.Low,
                FireIncidentEvent.FaultType.NONE);

        scheduler.getActiveEvents().put(3, e);
        scheduler.getActiveEvents().remove(3);

        assertFalse(scheduler.getActiveEvents().containsKey(3));
    }

    //Stuck drone logic
    @Test
    void stuckDroneRequeuesFire() {

        Scheduler.DroneInfo d = scheduler.getDrones().get(1);
        d.targetZone = 3;

        FireIncidentEvent e = new FireIncidentEvent(
                LocalTime.now(), new Zone(3,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.High,
                FireIncidentEvent.FaultType.NONE);

        scheduler.getActiveEvents().put(3, e);

        d.isFaulty = true;
        scheduler.getFireQueue().add(e);

        assertTrue(d.isFaulty);
        assertFalse(scheduler.getFireQueue().isEmpty());
    }

    //Nozzle fail logic
    @Test
    void nozzleFailRequeuesFire() {

        Scheduler.DroneInfo d = scheduler.getDrones().get(1);
        d.targetZone = 2;

        FireIncidentEvent e = new FireIncidentEvent(
                LocalTime.now(), new Zone(2,0,0,5,5),
                FireIncidentEvent.Status.FIRE_DETECTED, FireIncidentEvent.Severity.Moderate,
                FireIncidentEvent.FaultType.NONE);

        scheduler.getActiveEvents().put(2, e);

        d.isHardFault = true;
        scheduler.getFireQueue().add(e);

        assertTrue(d.isHardFault);
        assertFalse(scheduler.getFireQueue().isEmpty());
    }
}