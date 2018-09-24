package shared.msg;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;

public abstract class Message {
    public static transient final String SERVICE_NAME = "YACA_ADMIN";
    public static transient final String MESSAGE_END = "end";
    static transient final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private transient StringBuilder bldr = new StringBuilder();
    private long timestamp;

    public Message() {
        this.timestamp = new Date().getTime();
    }

    public String constructMessageString(){
        if (bldr == null) {
            bldr = new StringBuilder();
        }
        bldr.delete(0, bldr.length());

        bldr.append(getHeader());
        bldr.append("\r\n\r\n");
        bldr.append(getBody());
        bldr.append("\r\nend\r\n");
        return bldr.toString();
    }

    public static Message tryParse(String string){
        if (string == null) {
            return null;
        }
        String header = string.split("\r\n")[0];
        if (ChatMessage.HEADER.equals(header)){
            return gson.fromJson(extractJsonBody(string), ChatMessage.class);
        }
        if (LoginRequestMessage.HEADER.equals(header)){
            return gson.fromJson(extractJsonBody(string), LoginRequestMessage.class);
        }
        if (ServiceMessage.HEADER.equals(header)){
            return gson.fromJson(extractJsonBody(string), ServiceMessage.class);
        }
        else {
            return null;
        }
    }

    abstract String getHeader();

    abstract String getBody();

    abstract void clear();

    private static String extractJsonBody(String string) {
        String[] splitted = string.split("\r\n");
        StringBuilder bldr = new StringBuilder();
        for (int i = 2; i < splitted.length - 1; i++){
            bldr.append(splitted[i]);
        }
        return bldr.toString().equals("") ? "{}" : bldr.toString();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
