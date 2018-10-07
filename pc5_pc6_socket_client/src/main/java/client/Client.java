package client;

import shared.BaseClient;
import shared.conf.Conf;
import shared.msg.Message;
import shared.msg.ServiceMessage;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class Client extends BaseClient {
    private ActionHandler onMessageReceived;
    private MulticastSocket multicastSocket;

    public Client(Socket socket){
        super(socket);
        this.mySocket = socket;
        try {
            this.multicastSocket = new MulticastSocket(Conf.UDP_PORT);
            this.multicastSocket.joinGroup(Conf.MULTICAST_GROUP_ADDRESS);
        } catch (IOException e) {
            System.out.println("Unable to start broadcast listener; server broadcast messages will be unreachable");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Thread broadCastListenerThread = new Thread(() -> {
            byte[] receiveData = new byte[1024];
            while (!timeToStop) {
                Arrays.fill(receiveData, (byte) 0);
                DatagramPacket receive = new DatagramPacket(receiveData, receiveData.length);
                try {
                    multicastSocket.receive(receive);
                } catch (IOException e) {
                    System.out.println("IO exception occurred while reading broadcast message");
                }
                System.out.println("received broadcast:   " + new String(receive.getData(), 0, receive.getLength()));
                handleInput(new String(receive.getData(), 0, receive.getLength()));
            }
        });
        broadCastListenerThread.start();
        super.run();
    }

    @Override
    public void close(String reason) {
        this.close();
    }

    @Override
    public void close() {
        super.close();
        try {
            osw.close();
            br.close();
            multicastSocket.leaveGroup(Conf.MULTICAST_GROUP_ADDRESS);
            mySocket.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    @Override
    protected void handleInput(String input){
        System.out.println("Message received: " + input);
        Message msg = Message.tryParse(input);
        if (msg == null) {
            return;
        }
        onMessageReceived.onAction(msg);
    }

    @Override
    protected void onIOExceptionInRun(Exception e) {
        // send message to self
        ServiceMessage msg = new ServiceMessage(ServiceMessage.Type.ERROR, e.getLocalizedMessage());
        onMessageReceived.onAction(msg);
        close();
    }

    public void setOnMessageReceived(ActionHandler onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @FunctionalInterface
    interface ActionHandler {
        void onAction(Message msg);
    }
}
