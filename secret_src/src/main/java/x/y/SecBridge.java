package x.y;

public class SecBridge {

    private SecBridge() {
        // Prevent instantiation
    }

    public static byte[] o(byte[] in) {
        return in; // or your pure-Java logic
    }

    public static byte[] r(byte[] in) {
        return in; // or your pure-Java logic
    }

}
