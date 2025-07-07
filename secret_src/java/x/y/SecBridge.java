package x.y;

public class SecBridge {
    static { System.loadLibrary("loop"); }

    public static native byte[] o(byte[] in);  // obfuscate
    public static native byte[] r(byte[] in);  // rebuild
}
