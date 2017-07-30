package dto;

public class GeoData {

    private long mac;
    private byte pt;
    private int id;
    private long time;
    private double latitude;
    private double longitude;
    private int clientPort;

    public GeoData(long mac, byte pt, int id, long time, double latitude, double longitude, int clientPort) {
        this.mac = mac;
        this.pt = pt;
        this.id = id;
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
        this.clientPort = clientPort;
    }

    public long getMac() {return mac; }

    public byte getPt() {
        return pt;
    }

    public int getId() {
        return id;
    }

    public long getTime() {
        return time;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getClientPort() {return clientPort; }

    @Override
    public String toString() {
        return "GeoData {" +
                "mac = " + mac +
                ", pt = " + pt +
                ", id = " + id +
                ", time = " + time +
                ", latitude = " + latitude +
                ", longitude = " + longitude +
                ", clientPort = " + clientPort +
                '}';
    }
}
