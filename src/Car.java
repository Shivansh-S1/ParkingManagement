public class Car extends Vehicle {
    public Car(String number, String owner) { super(number, owner); }
    @Override public String getVehicleType() { return "Car"; }
    @Override public double calculateFee(long minutes) {
        return Math.max(1, (long)Math.ceil(minutes / 60.0)) * 30.0;
    }
}
