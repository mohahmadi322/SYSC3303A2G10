import java.text.DateFormat;
import java.time.LocalTime;
public class FireIncidentEvent {
    public enum Status{
        FIRE_DETECTED,
        DRONE_REQUESTED
    }
    public enum Severity{
        High,
        Moderate,
        Low

    }

    private LocalTime time;

    private Status status;
    private Zone zone;
    private Severity severity;

    public FireIncidentEvent(LocalTime time,Zone zone,Status incident, Severity severity){
        this.time = time;
        this.status = incident;
        this.severity = severity;
        this.zone = zone;
    }

    public LocalTime getTime() {
        return time;
    }

    public Status getStatus() {
        return status;
    }
    public void changeStatus(Status status) {
        this.status = status;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Zone getZone() {
        return zone;
    }

    public String toString(){
        return "Time:"+ time.toString() + " | " + "Zone:" + zone + " | " + "Event type:"+
                status + " | " + "Severity:" + severity;
    }
}
