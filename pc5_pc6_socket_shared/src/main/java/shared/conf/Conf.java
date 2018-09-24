package shared.conf;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Conf {
    public static final String HOST_NAME = "WINDOWS-21GELVO";
    public static final String SERVICE_NAME = "YACA_ADMIN";
    public static final int TCP_PORT = 33_333;
    public static final int UDP_PORT = 33_444;
    public static final InetAddress MULTICAST_GROUP_ADDRESS;

    static {
        try {
            MULTICAST_GROUP_ADDRESS = InetAddress.getByName("230.0.0.0");
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to initialize broadcast gruop address", e);
        }
    }
}
