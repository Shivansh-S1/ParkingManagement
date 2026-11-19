public class Bike extends Vehicle {
    public Bike(String number, String owner) { super(number, owner); }
    @Override public String getVehicleType() { return "Bike"; }
    @Override public double calculateFee(long minutes) {
        return Math.max(1, (long)Math.ceil(minutes / 60.0)) * 10.0;
    }
}
