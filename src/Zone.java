public class Zone {

    private boolean fireActive;

    private int id;
    private int startX;
    private int startY;
    private int endX;
    private int endY;


    FireIncidentEvent currentEvent;

    public Zone(int id, int startX, int startY, int endX, int endY) {
        this.id = id;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.fireActive = false;
        this.currentEvent = null;
    }

    public int getId() { return id; }

    public boolean isFireActive() { return fireActive; }

    public void activeFire() {
        this.fireActive = true;
    }

    public void fireExtinguished() {
        this.fireActive = false;
        this.currentEvent = null;
    }

    public int getEndX() {
        return endX;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getEndY() {
        return endY;
    }

    public String toString(){
        return " " + id;
    }


}
