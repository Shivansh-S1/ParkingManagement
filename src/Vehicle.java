public abstract class Vehicle {
    private final String registrationNumber;
    private final String ownerName;

    protected Vehicle(String registrationNumber, String ownerName) {
        this.registrationNumber = registrationNumber.trim().toUpperCase();
        this.ownerName = ownerName.trim().replace(",", " ");
    }

    public String getRegistrationNumber() { return registrationNumber; }
    public String getOwnerName() { return ownerName; }
    public abstract String getVehicleType();
    public abstract double calculateFee(long minutes);

    public String getDisplayName() {
        return getVehicleType() + " - " + registrationNumber;
    }
}
