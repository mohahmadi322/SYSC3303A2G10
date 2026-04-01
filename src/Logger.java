import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.*;
/**
 * Logger.java
 * <p>
 * Simple in-memory logger that periodically flushes
 * log events to the terminal using a daemon thread.
 *
 * Based on the EventLogger.java file provided by Dr Rami Sabouni.
 *
 * @author Mohammad Ahmadi
 * @author Dr Rami Sabouni
 */
public class Logger {
    /**
     * Thread-safe queue used to store log events.
     */
    private final ConcurrentLinkedQueue<LogEvent> queue =
            new ConcurrentLinkedQueue<>();

    /**
     * Scheduler used to periodically flush events from memory
     * to the terminal.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * Formatter used to create timestamps.
     */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Output file for logs
     */
    private static final String OUTPUT_FILE = "output.txt";



    /**
     * Constructor
     * <p>
     * Creates a background daemon thread that periodically flushes
     * the in-memory log queue.
     *
     * @param periodMs interval between flush operations in milliseconds
     */
    public Logger(long periodMs) {

        // Create daemon scheduler thread
        ThreadFactory factory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "log-flusher-daemon");
                t.setDaemon(true); // make it a daemon thread
                return t;
            }
        };

        // Create a scheduler with one background thread
        scheduler = Executors.newSingleThreadScheduledExecutor(factory);

        // Schedule periodic execution of the flush() method.
        scheduler.scheduleAtFixedRate(
                this::flush,      // method to execute
                periodMs,         // initial delay before first run
                periodMs,         // interval between runs
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Adds a new event to the log queue.
     * <p>
     * This method is intentionally very lightweight so that
     * application threads do not spend time performing I/O.
     *
     * @param entity    component generating the event
     * @param code type of event
     * @param data      optional extra information
     */
    public void log(String entity, String code, String... data) {
        // Create a new log event and insert it into the queue
        queue.add(new LogEvent(LocalDateTime.now(), entity, code, data));
    }


    /**
     * Flush operation executed by the background scheduler thread.
     * <p>
     * This method drains the queue and prints all events
     * currently waiting in memory.
     */
    public synchronized void flush() {
        LogEvent e;
        //Flush into text file
        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(OUTPUT_FILE, true))) {
            while ((e = queue.poll()) != null) {
                System.out.println(e.format());
                bw.write(e.format());
                bw.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Optional shutdown method.
     * <p>
     * Stops the scheduler thread and performs a final flush.
     * <p>
     * Since the scheduler thread is a daemon thread,
     * this method is not strictly required for program termination,
     * but it ensures no events remain unprinted.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        flush(); // final flush
    }
    /**
     * Call when fire has been received from the FireIncidentSubsystem.
     */
    public void fireReceived(int zoneId) {
        log("Scheduler", "FIRE_RECEIVED", String.valueOf(zoneId));
    }

    /**
     * Call when a drone has been assigned to a fire.
     */
    public void droneAssigned(int droneId, int zoneId) {
        log("Drone_" + droneId, "DRONE_ASSIGNED", String.valueOf(zoneId));
    }

    /**
     * Call when drone has arrived to fire zone.
     */
    public void droneArrived(int droneId, int zoneId) {
        log("Drone_" + droneId, "DRONE_ARRIVED", String.valueOf(zoneId));
    }

    /**
     * Call when fire is extinguished.
     */
    public void fireExtinguished(int droneId, int zoneId) {
        log("Drone_" + droneId, "FIRE_EXTINGUISHED", String.valueOf(zoneId));
    }

    /**
     * Call when drone is busy.
     */
    public void droneBusy(int droneId) {

        log("Drone_" + droneId, "DRONE_BUSY");
    }

    /**
     * Log when drone is idle.
     */
    public void droneIdle(int droneId) {

        log("Drone_" + droneId, "DRONE_IDLE");
    }

}
