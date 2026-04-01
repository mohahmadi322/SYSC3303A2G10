import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Analyzer class used to analyze the log events and measure the threads and print the outcomes.
 */
public class Analyzer {

    //Format of times of events.
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    //Array list of all events in the logs
    private ArrayList<LogEvent> logEvents = new ArrayList<>();

    private static final String INPUT_FILE = "output.txt";


    /**
     * Parse the events from the text file into logEvents arraylist.
     * @return ArrayList of LogEvent objects.
     */
    private ArrayList<LogEvent> parseFile(){
        try (BufferedReader br = new BufferedReader(new FileReader(INPUT_FILE))) {

            String line;

            while ((line = br.readLine()) != null) {

                //remove prefix and brackets
                line = line.replace("Log Event: [", "")
                        .replace("]", "");

                String[] values = line.split(",",-1);

                //get time
                LocalDateTime time = LocalDateTime.parse(values[0].trim(), FORMATTER);
                String entity = values[1].trim();//get the entity from event
                String code = values[2].trim();//get the code of the event

                //If there is a component placed by agent or technician, retrieve it from event.
                String component1 = values.length > 3 ? values[3].trim() : "";
                String component2 = values.length > 4 ? values[4].trim() : "";

                //Create new LogEvent object.
                LogEvent event = new LogEvent(time, entity, code, component1, component2);
                logEvents.add(event);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return logEvents;
    }

    /**
     * Computes response time for every zone
     */
    public void calculateResponseTime() {
        //Fire events and their time.
        HashMap<Integer, LocalDateTime> fireReceivedTimes = new HashMap<>();
        //The response times.
        ArrayList<Double> responseTimes = new ArrayList<>();

        for (LogEvent e : logEvents) {
            //Finding fire received events. start.
            if (e.code.equals("FIRE_RECEIVED")) {
                int zone = Integer.parseInt(e.data[0]);
                fireReceivedTimes.putIfAbsent(zone, e.time);
            }
            //Finding drone arrived event. End. response.
            if (e.code.equals("DRONE_ARRIVED")) {

                int zone = Integer.parseInt(e.data[0]);
                //If this event is already in waiting hashmap that means it is a response to an event from another
                //entity and it had been notified.
                if (fireReceivedTimes.containsKey(zone)) {
                    //Get the response time.
                    double time = Duration.between(
                            fireReceivedTimes.remove(zone), e.time).toMillis();
                    //Add it to the time array.
                    responseTimes.add(time);
                    System.out.printf("Zone %d response time is %.2f\n",
                            zone, time / 1000.0);
                }
            }
        }

        System.out.println();
        printAverageAndMax("Response time", responseTimes);
    }


    /**
     * Computes completion time for every zone:
     */
    public void calculateCompletionTime() {
        //Fire zones and the times they are received.
        HashMap<Integer, LocalDateTime> fireTime = new HashMap<>();
        //Array of fire completion times.
        ArrayList<Double> completionTimes = new ArrayList<>();

        for (LogEvent e : logEvents) {
            //Check times fire was received.
            if (e.code.equals("FIRE_RECEIVED")) {
                int zone = Integer.parseInt(e.data[0]);
                fireTime.putIfAbsent(zone, e.time);
            }
            //Check time fire was extinguished
            if (e.code.equals("FIRE_EXTINGUISHED")) {
                int zone = Integer.parseInt(e.data[0]);
                //Check if zone is on fire
                if (fireTime.containsKey(zone)) {
                    double time = Duration.between(
                            fireTime.remove(zone), e.time).toMillis();
                    completionTimes.add(time);
                    System.out.printf("Zone %d completion time is %.2f\n",
                            zone, time / 1000.0);
                }
            }
        }
        System.out.println();
        printAverageAndMax("Completion Time", completionTimes);
    }


    /**
     * Calculate the utilization for each drone.
     */
    public void calculateUtilization() {
        //Find the total time of the threads executing.
        LocalDateTime start = logEvents.get(0).time;
        LocalDateTime end   = logEvents.get(logEvents.size() - 1).time;
        double total = Duration.between(start, end).toMillis();
        //Record each busy event for an entity and their time.
        HashMap<String, LocalDateTime> busy = new HashMap<>();
        //Hold each the total busy time for each entity.
        HashMap<String, Double>totalBusy = new HashMap<>();

        for (LogEvent e : logEvents) {
            if (e.code.equals("DRONE_BUSY")) {
                //add busy time
                busy.putIfAbsent(e.entity, e.time);
            }
            else if (e.code.equals("DRONE_IDLE")) {
                if (busy.containsKey(e.entity)) {

                    double dur = Duration.between(
                            busy.remove(e.entity), e.time).toMillis();
                    totalBusy.merge(e.entity, dur, Double::sum);
                }
            }
        }

        for (Map.Entry<String, LocalDateTime> open : busy.entrySet()) {
            double ms = Duration.between(open.getValue(), end).toMillis();
            //Using merge method add the current wait time to the previous, getting the total..
            totalBusy.merge(open.getKey(), ms, Double::sum);
        }

        List<String> droneKeys = new ArrayList<>(totalBusy.keySet());
        droneKeys.sort(Comparator.comparingInt(Analyzer::droneNumber));

        for (String drone : droneKeys) {
            double busyTime  = totalBusy.get(drone);
            double util    = (busyTime / total) * 100.0;
            double idle    = 100.0 - util;
            System.out.printf("%s  active: %.2f  idle: %.2f\n",
                    drone, util, idle);
        }
    }

    /**
     * Prints the maximum and average values for the metric.
     * @param metric
     * @param times
     */
    private static void printAverageAndMax(String metric, ArrayList<Double> times) {
        if (times.isEmpty()) {
            System.out.println("No data for " + metric);
            return;
        }
        double sum = 0, max = 0;
        for (double t : times) {
            sum += t;
            if (t > max) max = t;
        }
        System.out.printf("Average %s is %.2f \n", metric, (sum / times.size()) / 1000.0);
        System.out.printf("Maximum %s is %.2f \n", metric, max / 1000.0);
    }

    /**
     * Get the number of the drone from the event.
     * @param entity
     * @return
     */
    private static int droneNumber(String entity) {
        try   {
            //get integer from the string
            return Integer.parseInt(entity.replace("Drone_", ""));
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }


    public void run() {
        parseFile();

        if (logEvents.isEmpty()) {
            System.out.println("No logs found.");
            return;
        }
        LocalDateTime simStart = logEvents.get(0).time;
        LocalDateTime simEnd   = logEvents.get(logEvents.size() - 1).time;
        double total = Duration.between(simStart, simEnd).toMillis() / 1000.0;

        System.out.printf("Simulation duration: %.2f \n", total);
        System.out.printf("Log entries parsed: %d\n", logEvents.size());

        System.out.println("\n------------------ Event Response Time ------------------");
        calculateResponseTime();

        System.out.println("\n----------------- Event Completion Time -----------------");
        calculateCompletionTime();

        System.out.println("\n--------------------- Drone Utilization ---------------------");
        calculateUtilization();

        System.out.println("=============================================================\n");
    }

    public static void main(String[] args) {
        new Analyzer().run();
    }
}
