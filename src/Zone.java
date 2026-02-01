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


    FireIncidentEvent currentEvent;// The current fire event that is happening in this zone/

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

    /**
     * Return a string representation of the zone object with the zone ID.
     * @return String representation of the zone object with the zone ID.
     */
    public String toString(){
        return " " + id;
    }

}
