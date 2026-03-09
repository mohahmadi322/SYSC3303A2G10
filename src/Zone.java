import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Zone class. Represent a zone in the system. Values come from the csv file read by the FireIncidentSubsystem.
 * Contains the ID and coordinates of the zone.
 * @author Mohammad Ahmadi 101267874
 * @date 2026-01-31
 */
public class Zone {
    private boolean fireActive;//Boolean flag that checks if a fire is active in this zone
    private int id;//The zone ID/number
    private int startX;//X coordinate of the start of zone
    private int startY;//Y coordinate of the start of zone
    private int endX;//X coordinate of the end of zone
    private int endY;//Y coordinate of the end of zone

    public static HashMap<Integer, Zone> zonesHash = new HashMap<>();//Hashmap of zones. The objects come from the csv file. The integer specifies the zone number and zone is the object.

    private static ArrayList<Zone> zones = new ArrayList<>();
    private FireIncidentEvent currentEvent;// The current fire event that is happening in this zone/

    /**
     * Constructor for zone. Inputs come from CSV file read by FireIncidentSubsystem.
     * @param id The zone ID/Number.
     * @param startX X coordinate of the start of zone
     * @param startY Y coordinate of the start of zone
     * @param endX X coordinate of the end of zone
     * @param endY Y coordinate of the end of zone
     */
    public Zone(int id, int startX, int startY, int endX, int endY) {
        this.id = id;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.fireActive = false;
        this.currentEvent = null;

    }
    /**
     * Getter method for the zone ID.
     * @return The ID of the zone.
     */
    public int getId() { return id; }

    /**
     * Checks if a fire is active in this zone.
     * @return True if zone is active, false is not.
     */
    public boolean isFireActive() { return fireActive; }

    /**
     * Signal that fire is active in this zone.
     */
    public void activeFire() {
        this.fireActive = true;
    }

    /**
     * Signal that fire has been extinguished in this zone.
     */
    public void fireExtinguished() {
        this.fireActive = false;
        this.currentEvent = null;
    }

    /**
     * Getter method for X coordinate of the end of zone
     * @return X coordinate of the start of zone
     */
    public int getEndX() {
        return endX;
    }
    /**
     * Getter method for X coordinate of the start of zone
     * @return X coordinate of the start of zone
     */
    public int getStartX() {
        return startX;
    }

    /**
     * Getter method for Y coordinate of the start of zone
     * @return Y coordinate of the start of zone
     */
    public int getStartY() {
        return startY;
    }
    /**
     * Getter method for Y coordinate of the end of zone
     * @return Y coordinate of the start of zone
     */
    public int getEndY() {
        return endY;
    }
    public void addZone(Zone zone){
        zones.add(zone);
    }

    /**
     * Return a string representation of the zone object with the zone ID.
     * @return String representation of the zone object with the zone ID.
     */
    public String toString(){
        return " " + id;
    }

    /**
     * Parse the zones from the csv file.
     * @param filePath The path of the file containing the zones.
     * @return A HashMap structure containing the zones and their zone id.
     */
    public static HashMap<Integer, Zone> parseZones(String filePath) {
        HashMap<Integer, Zone> zones = new HashMap<>();
        try (BufferedReader bf = new BufferedReader(new FileReader(filePath))) {
            String line;
            bf.readLine(); // Skip the first line of the file
            while ((line = bf.readLine()) != null) {
                String[] values = line.split(",");
                Zone zone = readZoneFile(values);//Call the readZoneFile method to create an object.
                zonesHash.put(zone.getId(), zone);
                zone.addZone(zone);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return zones;
    }

    /**
     * Create Zone from a row of the csv file.
     * @param values The row from csv file holding the values of the Zone.
     * @return The Zone from the file.
     */
    public static Zone readZoneFile(String[] values){
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

    public static Zone findZoneById(int id){
        for(Zone z : zones){
            if(z.getId() == id)
                return z;
        }
        return null;
    }
}
