import java.time.LocalTime;
public class FireIncidentEvent {
    public enum Incident{
        FIRE_DETECTED,
        DRONE_REQUESTED
    }
    public enum Severity{
        High,
        Moderate,
        Low

    }

    public LocalTime time;

    public Incident incident;
    public int zone;
    public Severity severity;

    public FireIncidentEvent(LocalTime time,Incident incident, Severity severity, int zone){
        this.time = time;
        this.incident = incident;
        this.severity = severity;
        this.zone = zone;
    }








}
