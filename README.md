# SmartPark – Parking Management System

A Java Swing desktop application demonstrating Object-Oriented Programming through a practical parking-lot workflow.

## Features
- **Interactive dashboard:** stat cards, animated occupancy bar and a visual slot map (green = free, red = occupied). Hover a slot for owner/fee details, click a free slot to start parking, click an occupied slot to check out.
- **Live updates:** clock, parked duration and running fee tick every second.
- **Smart entry form:** type selector showing free slots per type, auto-uppercase registration, live validation, next-slot preview, Enter-key flow.
- **Live billing:** pick a parked vehicle on the Exit page and watch the bill update before paying; receipt dialog on checkout.
- **Active Vehicles tab:** sortable table with live duration and fee (double-click a row to check out).
- **History tab:** search/filter, sortable columns, revenue totals and CSV export.
- **Light/Dark theme** toggle and keyboard shortcuts (Ctrl+1 to Ctrl+5).
- **Persistence:** history in `data/parking-history.csv`; vehicles still parked are saved in `data/active-sessions.csv` and restored on restart.
- Duplicate-registration prevention, input validation and exception handling.

## Requirements
- JDK 8 or newer
- Windows, macOS or Linux

## Run on Windows
1. Extract the ZIP.
2. Open the extracted `SmartParkingManagement` folder.
3. Double-click `run.bat`, or open Command Prompt in this folder and run:
   ```bat
   run.bat
   ```

## Run on macOS/Linux
From this folder:
```bash
chmod +x run.sh
./run.sh
```

## Run manually
```bash
javac -encoding UTF-8 -d out src/*.java
java -cp out Main
```

## Parking rules
- 6 Car slots, 6 Bike slots and 3 EV slots.
- Car: Rs. 30 per hour.
- Bike: Rs. 10 per hour.
- Electric Vehicle: Rs. 20 per hour.
- Any started hour is charged as a complete hour, with a minimum one-hour charge.

## OOP concepts used
1. **Encapsulation:** Vehicle fields are private and exposed through getters.
2. **Abstraction:** `Vehicle` is an abstract class with abstract methods.
3. **Inheritance:** `Car`, `Bike` and `ElectricVehicle` extend `Vehicle`.
4. **Runtime polymorphism:** `calculateFee()` and `getVehicleType()` are overridden and invoked through `Vehicle` references.
5. **Composition:** `ParkingManager` owns parking slots and active parking records.
6. **Exception handling:** Invalid inputs, duplicate entries and unavailable slots are handled.

## Project structure
```text
SmartParkingManagement/
├── src/
│   ├── Vehicle.java
│   ├── Car.java
│   ├── Bike.java
│   ├── ElectricVehicle.java
│   ├── ParkingSlot.java
│   ├── ParkingTicket.java
│   ├── ParkingManager.java
│   └── Main.java
├── data/
├── run.bat
├── run.sh
└── README.md
```

## Data files
Completed transactions and active sessions are saved under `data/` and loaded at startup, so nothing is lost when the app is closed.
