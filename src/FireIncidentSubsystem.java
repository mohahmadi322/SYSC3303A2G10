import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class FireIncidentSubsystem implements Runnable {
    private static String INCIDENT_FILEPATH = "Event_File.csv";

    private static String ZONE_FILEPATH= "Zone_File.csv";
    Scheduler scheduler;
    ArrayList<FireIncidentEvent> incidents;
    HashMap<Integer, Zone> zones;

    public FireIncidentSubsystem(Scheduler scheduler){
        this.scheduler = scheduler;
        incidents = new ArrayList<>();
        zones = new HashMap<>();

    }
    public FireIncidentEvent readIncidentEvent(String[] values){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime time = LocalTime.parse(values[0].trim(), formatter);
        int zoneID = Integer.parseInt(values[1].trim());
        FireIncidentEvent.Status status= FireIncidentEvent.Status.valueOf(values[2]);
        FireIncidentEvent.Severity severity= FireIncidentEvent.Severity.valueOf(values[3]);
        zones.get(zoneID).activeFire();
        return new FireIncidentEvent(time,zones.get(zoneID),status,severity);
    }

    public Zone readZoneFile(String[] values){
        int id = Integer.parseInt(values[0].trim());

        // "(0;0)" → "0;0" → ["0","0"]
        String[] start = values[1]
                .replace("(", "")
                .replace(")", "")
                .split(";");

        String[] end = values[2]
                .replace("(", "")
                .replace(")", "")
                .split(";");

        int startX = Integer.parseInt(start[0].trim());
        int startY = Integer.parseInt(start[1].trim());

        int endX = Integer.parseInt(end[0].trim());
        int endY = Integer.parseInt(end[1].trim());

        return new Zone(id, startX, startY, endX, endY);
    }
    public HashMap<Integer, Zone> parseZones(String filePath) {
        HashMap<Integer, Zone> zones = new HashMap<>();
        try (BufferedReader bf = new BufferedReader(new FileReader(filePath))) {
            String line;
            bf.readLine(); // skip header
            while ((line = bf.readLine()) != null) {
                String[] values = line.split(",");
                Zone zone = readZoneFile(values);
                zones.put(zone.getId(), zone);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return zones;
    }
    public ArrayList<FireIncidentEvent> parseIncidentFiles (String filePath){
        String[] values = new String[0];
        ArrayList<FireIncidentEvent> events = new ArrayList<>();
        try(BufferedReader bf = new BufferedReader(new FileReader(filePath))) {
            String line;
            bf.readLine();//Skip first line of csv file
            while((line = bf.readLine()) != null){
                values = line.split(",");
                if(readIncidentEvent(values) != null){events.add(readIncidentEvent(values));}
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        };
        return events;
    }
    public void firePutout(Zone zone){
        System.out.println("Fire subsystem: Fire is put out at zone " + zone.toString());
        zone.fireExtinguished();
    }
    @Override
    public void run() {
        zones = parseZones(ZONE_FILEPATH);
        incidents = parseIncidentFiles(INCIDENT_FILEPATH);
        for(FireIncidentEvent e: incidents){
            scheduler.newIncident(e);
        }
    }
}
