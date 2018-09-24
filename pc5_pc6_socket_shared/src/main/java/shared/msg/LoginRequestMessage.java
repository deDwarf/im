package shared.msg;

public class LoginRequestMessage extends Message {
    public static final transient String HEADER = "YECA/0.1 Login request";

    private String username;
    private String password;

    public LoginRequestMessage(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    String getHeader() {
        return HEADER;
    }

    @Override
    String getBody() {
        return gson.toJson(this);
    }

    @Override
    void clear() {
        this.username = null;
        this.password = null;
    }

    public static String getHEADER() {
        return HEADER;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
