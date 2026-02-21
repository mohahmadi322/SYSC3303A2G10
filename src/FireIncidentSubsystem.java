import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * FireIncidentSubsystem class. Represents the fire incident subsystem.
 * Responsible for reading event and zone csv files and creating instances of FireIncidentEvent and Zone according to
 * the specifications in the files.
 * Updates the active fire status of the zone after a drone has put out the fire.
 *
 * Communication with drone is done strictly through the scheduler class.
 *
 * Implements runnable interface as an active object.
 *
 * @author Mohammad Ahmadi 101267874
 * @date 2026-01-31
 */
public class FireIncidentSubsystem implements Runnable {

    Scheduler scheduler;//instance of scheduler class used for communication with drones.
    ArrayList<FireIncidentEvent> incidents;//array of Fire incident events. The objects come from the csv file.
    HashMap<Integer, Zone> zones;//Hashmap of zones. The objects come from the csv file. The integer specifies the zone number and zone is the object.
    private GUI gui;

    /**
     * Constructor for FireIncidentSubsystem class.
     * @param scheduler The central scheduler used for coordination
     */
    public FireIncidentSubsystem(Scheduler scheduler, GUI gui){
        this.scheduler = scheduler;
        this.gui = gui;
        incidents = new ArrayList<>();
        zones = new HashMap<>();
    }

    /**
     * Create FireIncidentEvent from a row of the csv file.
     * @param values The row from csv file holding the values of the FireIncidentEvent.
     * @return The FireIncidentEvent from the file.
     */
    public FireIncidentEvent readIncidentEvent(String[] values){
        /**Date formatter object to hold the time of the event. This object allows for easy formatting of the time, instead of having to use a string*/
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime time = LocalTime.parse(values[0].trim(), formatter);//Get time of event from the first column.
        int zoneID = Integer.parseInt(values[1].trim());//Get the zone from the second column.
        FireIncidentEvent.Status status= FireIncidentEvent.Status.valueOf(values[2]);//Get the status of the fire from the third column.
        FireIncidentEvent.Severity severity= FireIncidentEvent.Severity.valueOf(values[3]);//Get the severity from the fourth column.
        return new FireIncidentEvent(time,zones.get(zoneID),status,severity);//Return a FireIncidentEvent using the parameters from values.
    }

    /**
     * Create Zone from a row of the csv file.
     * @param values The row from csv file holding the values of the Zone.
     * @return The Zone from the file.
     */
    public Zone readZoneFile(String[] values){
        int id = Integer.parseInt(values[0].trim());// Get the id from the first column.


        /**Extracting the dimensions of the zones, "(0;0)" -> "0;0" -> ["0","0"]*/
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

    /**
     * Parse the zones from the csv file.
     * @param filePath The path of the file containing the zones.
     * @return A HashMap structure containing the zones and their zone id.
     */
    public HashMap<Integer, Zone> parseZones(String filePath) {
        HashMap<Integer, Zone> zones = new HashMap<>();
        try (BufferedReader bf = new BufferedReader(new FileReader(filePath))) {
            String line;
            bf.readLine(); // Skip the first line of the file
            while ((line = bf.readLine()) != null) {
                String[] values = line.split(",");
                Zone zone = readZoneFile(values);//Call the readZoneFile method to create an object.
                zones.put(zone.getId(), zone);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return zones;
    }

    /**
     * Parse the FireIncidentEvent from the csv file.
     * @param filePath The path of the file containing the fire incident events.
     * @return An ArrayList of FireIncidentEvent objects.
     */
    public ArrayList<FireIncidentEvent> parseIncidentFiles (String filePath){
        String[] values = new String[0];//Array of values read from a line on the csv.
        ArrayList<FireIncidentEvent> events = new ArrayList<>();
        try(BufferedReader bf = new BufferedReader(new FileReader(filePath))) {
            String line;
            bf.readLine();//Skip first line of csv file
            while((line = bf.readLine()) != null){
                values = line.split(",");
                if(readIncidentEvent(values) != null){events.add(readIncidentEvent(values));}//To ensure a null event is not added to the array
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        };
        return events;
    }

    /**
     * After drone has put out the fire, the scheduler sends a signal to the fire incident subsystem that the fire in
     * that zone is out.
     * @param zone The zone the drone was sent to.
     */
    public synchronized void firePutout(Zone zone){
        System.out.println("Fire subsystem: Fire is put out at zone " + zone.toString());
        gui.log("Fire subsystem: Fire is put out at zone " + zone.toString());
        zone.fireExtinguished();
        notifyAll();
    }

    /**
     * Parses the files and create the zones and fire incident events.
     * Calls the scheduler for every event that has been created.
     */
    @Override
    public void run() {
        zones = parseZones("Zone_File.csv");
        incidents = parseIncidentFiles("Event_File.csv");
        for(FireIncidentEvent e: incidents){
            scheduler.newIncident(e);
        }
    }
}
