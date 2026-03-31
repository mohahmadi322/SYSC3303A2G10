import java.io.*;
import java.net.*;
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
 * @author zeina Mouhtadi 101169685
 * @date 2026-01-31
 */
public class FireIncidentSubsystem implements Runnable {

    private ArrayList<FireIncidentEvent> incidents;//array of Fire incident events. The objects come from the csv file.
    DatagramSocket sendAndReceiveSocket;//One socket for sending and receiving data.

    /**
     * Constructor for FireIncidentSubsystem class.
     */
    public FireIncidentSubsystem(){
        incidents = new ArrayList<>();
        try {
            sendAndReceiveSocket = new DatagramSocket();
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        }
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
        FireIncidentEvent.FaultType faultType= FireIncidentEvent.FaultType.valueOf(values[4]);//Get the severity from the fourth column.
        return new FireIncidentEvent(time,Zone.zonesHash.get(zoneID),status,severity, faultType);//Return a FireIncidentEvent using the parameters from values.
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
        zone.fireExtinguished();
        notifyAll();
    }

    /**
     * Send fire incident events to scheduler.
     * @param e Event to be sent to scheduler.
     * @throws UnknownHostException
     */
    private void sendFireIncident(FireIncidentEvent e) throws UnknownHostException {
        String message = e.serialize();

        byte[] data = message.getBytes();

        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                InetAddress.getLocalHost(),
                5000
        );

        try{
            sendAndReceiveSocket.send(packet);
        }catch(SocketException exception){} catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        System.out.println("Sent: " + message);
    }

    /**
     * Parses the files and create the zones and fire incident events.
     * Calls the scheduler for every event that has been created.
     */
    @Override
    public void run() {
        incidents = parseIncidentFiles("Event_File.csv");
        for(FireIncidentEvent e: incidents){
            try {
                sendFireIncident(e);
            } catch (UnknownHostException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static void main(String[] args) {
        Zone.parseZones("Zone_File.csv");
        Thread fireIncident = new Thread(new FireIncidentSubsystem());
        fireIncident.start();
    }
}
