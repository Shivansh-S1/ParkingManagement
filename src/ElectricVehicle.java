public class ElectricVehicle extends Vehicle {
    public ElectricVehicle(String number, String owner) { super(number, owner); }
    @Override public String getVehicleType() { return "Electric Vehicle"; }
    @Override public double calculateFee(long minutes) {
        return Math.max(1, (long)Math.ceil(minutes / 60.0)) * 20.0;
    }
}
