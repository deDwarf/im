package shared;

import shared.msg.Message;

public interface IClient {
    void close(String reason);

    void close();

    void sendMessage(Message message);

    String getName();

    boolean isConnected();

    String getAddress();
}
