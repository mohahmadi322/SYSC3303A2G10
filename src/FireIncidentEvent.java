
import java.time.LocalTime;

/**
 * Class that represents a fire incident.
 * Contains the zones, status, time, and severity of the incident.
 * @author Mohammad Ahmadi 101267874
 * @date 2026-01-31
 */
public class FireIncidentEvent {
    public enum Status{//Event lifecycle
        FIRE_DETECTED,
        DRONE_REQUESTED
    }
    public enum Severity{//Severity of the event.
        High,
        Moderate,
        Low

    }

    private LocalTime time;//Time of the event
    private Status status;
    private Zone zone;//Zone object that the fire is at.
    private Severity severity;

    /**
     * Constructor of the class. Takes in the inputs from an Event csv file.
     * @param time Time of event.
     * @param zone Zone fire is located.
     * @param status Status of the event.
     * @param severity Severity of the event.
     */
    public FireIncidentEvent(LocalTime time,Zone zone,Status status, Severity severity){
        this.time = time;
        this.status = status;
        this.severity = severity;
        this.zone = zone;
    }

    /**
     * Getter method for the time.
     * @return The time of the event.
     */
    public LocalTime getTime() {
        return time;
    }

    /**
     * Getter method for status of the event.
     * @return Status of the event.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Changes the status of the event.
     * @param status The status we want to change to.
     */
    public void changeStatus(Status status) {
        this.status = status;
    }

    /**
     * Getter for the severity of the event.
     * @return The severity of the method.
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Getter for the zone.
     * @return the zone event is located.
     */
    public Zone getZone() {
        return zone;
    }

    /**
     * Create a string representation of the event.
     * @return A string representing the event.
     */

    public String toString(){
        return "Time:"+ time.toString() + " | " + "Zone:" + zone + " | " + "Event type:"+
                status + " | " + "Severity:" + severity;
    }
}
