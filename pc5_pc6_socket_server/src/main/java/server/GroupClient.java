package server;

import shared.IClient;
import shared.msg.ChatMessage;
import shared.msg.Message;

import java.util.HashSet;
import java.util.Set;

public class GroupClient implements IClient {

    private String name;
    private Set<IClient> listeners;

    public GroupClient(String name) {
        this.name = name;
        listeners = new HashSet<>();
    }

    public void subscribe(IClient client) {
        listeners.add(client);
    }

    public void unSubscribe(IClient client) {
        listeners.remove(client);
    }

    @Override
    public void close(String reason) { }

    @Override
    public void close() { }

    @Override
    public void sendMessage(Message message) {
        if (! (message instanceof ChatMessage)) {
            return;
        }
        ChatMessage castedMessage = (ChatMessage) message;
        String messageFrom = castedMessage.getFrom();
        castedMessage.setDestinationChat(name);
        for(IClient listener: listeners) {
            if (!listener.getName().equals(messageFrom)) {
                listener.sendMessage(castedMessage);
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public String getAddress() {
        return name;
    }
}
