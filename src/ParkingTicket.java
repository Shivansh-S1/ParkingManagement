import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ParkingTicket {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private final String ticketId;
    private final Vehicle vehicle;
    private final int slotNumber;
    private final LocalDateTime entryTime;

    public ParkingTicket(String ticketId, Vehicle vehicle, int slotNumber) {
        this(ticketId, vehicle, slotNumber, LocalDateTime.now());
    }

    /** Used when restoring a session that was still active when the app was closed. */
    public ParkingTicket(String ticketId, Vehicle vehicle, int slotNumber, LocalDateTime entryTime) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.slotNumber = slotNumber;
        this.entryTime = entryTime;
    }

    public String getTicketId() { return ticketId; }
    public Vehicle getVehicle() { return vehicle; }
    public int getSlotNumber() { return slotNumber; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public long getParkedMinutes() {
        return Math.max(1, Duration.between(entryTime, LocalDateTime.now()).toMinutes());
    }
    public double getFee() { return vehicle.calculateFee(getParkedMinutes()); }
    public String getEntryText() { return entryTime.format(FMT); }
    public static String nowText() { return LocalDateTime.now().format(FMT); }
}
