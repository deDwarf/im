package shared.msg;

public class ServiceMessage extends Message {
    public enum Type {
        INFO, REQUIRE_AUTH, AUTH_SUCCESS,
        AUTH_FAIL, ERROR, CONNECTION_CLOSED,
        UPDATE_ONLINE_LIST
    }
    public static final transient String HEADER = "YECA/0.1 Service";

    private Type msgType;
    private String message;

    public ServiceMessage(Type msgType, String message) {
        this.msgType = msgType;
        this.message = message;
    }

    public ServiceMessage(Type msgType) {
        this.msgType = msgType;
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
    void clear() { }

    public Type getMsgType() {
        return msgType;
    }

    public String getMessage() {
        return message;
    }

    public void setMsgType(Type msgType) {
        this.msgType = msgType;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
