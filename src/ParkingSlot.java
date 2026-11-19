public class ParkingSlot {
    private final int slotNumber;
    private final String category;
    private Vehicle vehicle;

    public ParkingSlot(int slotNumber, String category) {
        this.slotNumber = slotNumber;
        this.category = category;
    }

    public int getSlotNumber() { return slotNumber; }
    public String getCategory() { return category; }
    public Vehicle getVehicle() { return vehicle; }
    public boolean isOccupied() { return vehicle != null; }
    public boolean canFit(Vehicle v) {
        if (v instanceof Car) return category.equals("Car") || category.equals("Any");
        if (v instanceof Bike) return category.equals("Bike") || category.equals("Any");
        return category.equals("EV") || category.equals("Any");
    }
    public void park(Vehicle v) {
        if (isOccupied() || !canFit(v)) throw new IllegalStateException("Slot is not suitable or is occupied.");
        vehicle = v;
    }
    public void release() { vehicle = null; }
}
