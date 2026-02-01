SYSC3303 – Fire Incident Drone Simulation
Group 10
=========================================

This project simulates a fire‑response system where autonomous drones are dispatched
to extinguish fires detected across multiple geographic zones. The system includes:

• A Scheduler that assigns drones to fire events
• A Fire Incident Subsystem that reads and generates events
• A GUI that visualizes zones, fires, and drone activity
• A Drone subsystem that travels, extinguishes fires, and returns to base
• A JUnit test validating core functionality


------------------------------------------------------------
1. Project Structure and File Descriptions
------------------------------------------------------------

Main.java
---------
Entry point of the application. Creates the GUI, Scheduler, and starts the simulation.

GUI.java
--------
Handles all visual rendering:
• Displays zones as grid cells
• Shows fire severity (H/M/L) in red
• Shows drones (D#) in green/purple depending on state
• Removes indicators when events complete
• Provides logging output for simulation events

Scheduler.java
--------------
Central controller of the system:
• Receives new fire events
• Manages a queue of incidents
• Tracks available drones
• Assigns drones to events
• Updates the GUI when fires start or are extinguished

Drone.java
----------
Represents a single autonomous drone:
• Waits for events from the Scheduler
• Travels to the fire zone (simulated timing)
• Drops water based on severity
• Returns to origin
• Updates GUI during each phase

FireIncidentSubsystem.java
--------------------------
Responsible for:
• Loading fire events from CSV files
• Creating FireIncidentEvent objects
• Maintaining the list of zones
• Marking fires as active or extinguished

FireIncidentEvent.java
----------------------
Data model representing a single fire event:
• Timestamp
• Zone
• Severity (Low, Moderate, High)
• Status (FIRE_DETECTED, DRONE_REQUESTED, etc.)

Zone.java
---------
Represents a geographic zone:
• Coordinates (startX, startY, endX, endY)
• Tracks whether a fire is active
• Provides helper methods for GUI placement

Event_File.csv
--------------
Input file containing fire events in the format: Time,Zone ID,Event type,Severity

Zones.csv:
----------
--------------
Input file containing fire events in the format: ZoneID,StartX,StartY,EndX,EndY


SystemTest.java
---------------
JUnit tests validating:
• File parsing
• Zone assignment
• Scheduler–Drone communication
• Fire extinguishing logic


------------------------------------------------------------
2. Setup Instructions
------------------------------------------------------------

1. Install Java 17 or later.
2. Open the project in IntelliJ IDEA (recommended).
3. Build the project using IntelliJ 
5. Run Main.java to start the simulation.


------------------------------------------------------------
3. Running the Simulation
------------------------------------------------------------

1. Launch Main.java.
2. The GUI window will appear, showing all zones.
3. When a fire event is read:
   • The zone displays a red severity marker (H/M/L).
   • The Scheduler assigns an available drone.
   • The drone travels, extinguishes the fire, and returns.
   • The GUI updates throughout the process.

4. The console and GUI log panel show detailed event messages.


------------------------------------------------------------
4. Testing
------------------------------------------------------------

Run the JUnit tests in SystemTest.java to verify:

• CSV parsing
• Zone mapping
• Scheduler–Drone communication
• Fire extinguishing behavior

Tests can be run via IntelliJ


------------------------------------------------------------
5. Notes
------------------------------------------------------------

• The GUI uses updateOrReplaceSquare() to ensure indicators do not accumulate.
• The Scheduler uses wait()/notifyAll() for thread coordination.


------------------------------------------------------------
6. Authors
   Zeina Mouhtadi
   Mohammad Ahmadi
   Leena Ford 
------------------------------------------------------------

Team Responsibilities – Iteration 1
-----------------------------------
•Zeina: GUI design and integration, readme file
• Mohammad: Implemented the core code structure for the Scheduler, Drone, and Fire Incident Subsystem. 
Developed the event parsing and communication logic
• Leena: sequence and class diagrams


Versioning
----------
This submission corresponds to Iteration 1.
