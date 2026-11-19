import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;

public class ParkingManager {
    private final List<ParkingSlot> slots = new ArrayList<>();
    private final Map<String, ParkingTicket> activeTickets = new LinkedHashMap<>();
    private final List<String> history = new ArrayList<>();
    private final Path dataFile = Paths.get("data", "parking-history.csv");
    private final Path activeFile = Paths.get("data", "active-sessions.csv");

    public ParkingManager() {
        for (int i = 1; i <= 6; i++) slots.add(new ParkingSlot(i, "Car"));
        for (int i = 7; i <= 12; i++) slots.add(new ParkingSlot(i, "Bike"));
        for (int i = 13; i <= 15; i++) slots.add(new ParkingSlot(i, "EV"));
        loadHistory();
        loadActive();
    }

    public List<ParkingSlot> getSlots() { return Collections.unmodifiableList(slots); }
    public Collection<ParkingTicket> getActiveTickets() {
        return Collections.unmodifiableCollection(activeTickets.values());
    }
    public List<String> getHistory() { return Collections.unmodifiableList(history); }
    public ParkingTicket getTicket(String reg) {
        return reg == null ? null : activeTickets.get(reg.trim().toUpperCase());
    }

    /** Maps a vehicle type name to the slot category that serves it. */
    public static String categoryOf(String type) {
        return type.startsWith("Electric") || type.equals("EV") ? "EV" : type;
    }

    /** Factory: builds the right Vehicle subclass (runtime polymorphism). */
    public static Vehicle create(String type, String reg, String owner) {
        switch (categoryOf(type)) {
            case "Car":  return new Car(reg, owner);
            case "Bike": return new Bike(reg, owner);
            default:     return new ElectricVehicle(reg, owner);
        }
    }

    public int freeCount(String type) {
        String cat = categoryOf(type);
        int n = 0;
        for (ParkingSlot s : slots) if (s.getCategory().equals(cat) && !s.isOccupied()) n++;
        return n;
    }

    /** Slot number that would be allocated next for this type, or -1 if none is free. */
    public int peekSlot(String type) {
        String cat = categoryOf(type);
        for (ParkingSlot s : slots) if (s.getCategory().equals(cat) && !s.isOccupied()) return s.getSlotNumber();
        return -1;
    }

    public ParkingTicket parkVehicle(Vehicle vehicle) {
        String reg = vehicle.getRegistrationNumber();
        if (reg.isEmpty() || vehicle.getOwnerName().isEmpty())
            throw new IllegalArgumentException("Registration number and owner name are required.");
        if (!reg.matches("[A-Z0-9][A-Z0-9 \\-]{3,13}"))
            throw new IllegalArgumentException("Enter a valid registration number (4-14 characters, e.g. DL 01 AB 1234).");
        if (activeTickets.containsKey(reg))
            throw new IllegalArgumentException("This vehicle is already parked.");
        for (ParkingSlot slot : slots) {
            if (!slot.isOccupied() && slot.canFit(vehicle)) {
                slot.park(vehicle);
                String id = "PK" + System.currentTimeMillis();
                ParkingTicket ticket = new ParkingTicket(id, vehicle, slot.getSlotNumber());
                activeTickets.put(reg, ticket);
                saveActive();
                return ticket;
            }
        }
        throw new IllegalStateException("No suitable parking slot is available.");
    }

    public String exitVehicle(String registrationNumber) {
        String reg = registrationNumber.trim().toUpperCase();
        ParkingTicket ticket = activeTickets.remove(reg);
        if (ticket == null) throw new IllegalArgumentException("No active parking record found for this vehicle.");
        for (ParkingSlot slot : slots) {
            if (slot.getSlotNumber() == ticket.getSlotNumber()) { slot.release(); break; }
        }
        saveActive();
        long minutes = ticket.getParkedMinutes();
        double fee = ticket.getFee();
        String record = ticket.getTicketId() + "," + reg + "," +
            ticket.getVehicle().getVehicleType() + "," + ticket.getSlotNumber() + "," +
            ticket.getEntryText() + "," + minutes + "," + String.format(Locale.US, "%.2f", fee) +
            "," + ParkingTicket.nowText();
        history.add(0, record);
        saveRecord(record);
        return "Vehicle: " + reg + "\nOwner: " + ticket.getVehicle().getOwnerName() +
            "\nSlot: " + ticket.getSlotNumber() +
            "\nDuration: " + minutes + " minute(s)\nParking fee: Rs. " +
            String.format(Locale.US, "%.2f", fee);
    }

    private void saveRecord(String record) {
        try {
            Files.createDirectories(dataFile.getParent());
            Files.write(dataFile, Collections.singletonList(record),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new RuntimeException("Could not save parking history: " + ex.getMessage(), ex);
        }
    }

    /** Active sessions are saved so vehicles stay parked after the app is restarted. */
    private void saveActive() {
        List<String> lines = new ArrayList<>();
        for (ParkingTicket t : activeTickets.values())
            lines.add(t.getTicketId() + "," + t.getVehicle().getVehicleType() + "," +
                t.getVehicle().getRegistrationNumber() + "," + t.getVehicle().getOwnerName() + "," +
                t.getSlotNumber() + "," + t.getEntryTime());
        try {
            Files.createDirectories(activeFile.getParent());
            Files.write(activeFile, lines);
        } catch (IOException ex) {
            System.err.println("Could not save active sessions: " + ex.getMessage());
        }
    }

    private void loadActive() {
        try {
            if (!Files.exists(activeFile)) return;
            for (String line : Files.readAllLines(activeFile)) {
                try {
                    String[] c = line.split(",", -1);
                    Vehicle v = create(c[1], c[2], c[3]);
                    int slotNo = Integer.parseInt(c[4]);
                    for (ParkingSlot s : slots) {
                        if (s.getSlotNumber() == slotNo && !s.isOccupied()) {
                            s.park(v);
                            activeTickets.put(v.getRegistrationNumber(),
                                new ParkingTicket(c[0], v, slotNo, LocalDateTime.parse(c[5])));
                        }
                    }
                } catch (Exception skip) { /* ignore a corrupt line */ }
            }
        } catch (IOException ex) {
            System.err.println("Could not load active sessions: " + ex.getMessage());
        }
    }

    private void loadHistory() {
        try {
            if (Files.exists(dataFile)) {
                List<String> lines = Files.readAllLines(dataFile);
                Collections.reverse(lines);
                history.addAll(lines);
            }
        } catch (IOException ex) {
            System.err.println("Could not load history: " + ex.getMessage());
        }
    }
}
