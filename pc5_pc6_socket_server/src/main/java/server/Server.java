package server;

import shared.conf.Conf;
import shared.msg.ChatMessage;
import shared.msg.Message;
import shared.msg.ServiceMessage;
import util.ThreadSafe;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    enum RouteStatus {
        SUCCESS, USER_DOES_NOT_EXISTS, USER_OFFLINE;
    }

    public static final Server INSTANCE = new Server();

    private Map<String, ClientWorker> runningConnections;
    private ServerSocket serverSocket;
    private DatagramSocket multicastSocket;
    private Thread myThread;
    private Database db;

    private Server(){
        db = Database.getInstance();
        runningConnections = new ConcurrentHashMap<>();
    }

    public static Server getInstance(){
        return INSTANCE;
    }

    public void start(){
        try {
            serverSocket = new ServerSocket(Conf.TCP_PORT);
            multicastSocket = new DatagramSocket();
        } catch (IOException e) {
            serverSocket = null;
            throw new RuntimeException("Failed to initialize server instance. For reason: ", e);
        }
        if (myThread != null){
            return;
        }
        myThread = new MyThread();
        myThread.start();
    }

    public void stop(){
        if (myThread == null){
            return;
        }
        myThread.interrupt();
        try {
            // just a trick. see http://supremetechs.com/2013/07/16/java-serversocket-close-accept-throws-socket-closed-exception/
            new Socket(InetAddress.getLocalHost(), Conf.TCP_PORT);
            myThread.join(3000);
            serverSocket.close();
            runningConnections.forEach((s, client) -> {
                client.close("Server is shutting down");
            });
        } catch (InterruptedException | IOException e) {
            // do nothing
        }
        myThread = null;
    }

    @ThreadSafe
    public RouteStatus routeMessage(ChatMessage message){
        String user_to = message.getTo();
        String user_from = message.getFrom();
        if (user_to == null) {
            runningConnections.forEach((s, client) -> {
                if (!s.equals(user_from)){
                    client.sendMessage(message.constructMessageString());
                }
            });
            return RouteStatus.SUCCESS;
        }
        if (!db.checkUserExists(user_to)){
            return RouteStatus.USER_DOES_NOT_EXISTS;
        }
        ClientWorker destinationClient = this.runningConnections.get(user_to);
        if (destinationClient != null){
            destinationClient.sendMessage(message.constructMessageString());
            return RouteStatus.SUCCESS;
        }
        else {
            return RouteStatus.USER_OFFLINE;
        }
    }

    public String[] getOnlineUsers(){
        Object[] objectArray = runningConnections.keySet().toArray();
        return Arrays.copyOf(objectArray, objectArray.length, String[].class);
    }

    public void addActiveConnection(String username, ClientWorker client){
        runningConnections.put(username, client);
    }

    public void removeActiveConnection(String username){
        if (username == null){
            return;
        }
        runningConnections.remove(username);
    }

    void onCredentialsReceived(String username, String password, ClientWorker cl){
        ServiceMessage msgTemplate = new ServiceMessage(ServiceMessage.Type.INFO);
        ClientWorker anotherClient;
        if ((anotherClient = runningConnections.get(username)) != null){
            if (anotherClient.isConnected()){
                msgTemplate.setMsgType(ServiceMessage.Type.ERROR);
                msgTemplate.setMessage("Someone just tried to login using your credentials. Ip: " +
                        anotherClient.getSocket().getInetAddress().toString());
                anotherClient.sendMessage(msgTemplate.constructMessageString());
                cl.close(String.format("Authorization refused. User \"%s\" is already logged in", username));
                return;
            }
        }

        boolean accepted = db.checkCredentials(username, password);
        if (!accepted){
            cl.close("Invalid credentials");
            return;
        }
        cl.setAuthorized(username);
        addActiveConnection(username, cl);

        // at this point we know user is valid
        msgTemplate.setMsgType(ServiceMessage.Type.AUTH_SUCCESS);
        msgTemplate.setMessage(username);
        cl.sendMessage(msgTemplate.constructMessageString());

        msgTemplate.setMsgType(ServiceMessage.Type.UPDATE_ONLINE_LIST);
        msgTemplate.setMessage(String.join(",", Arrays.asList(getOnlineUsers())));
        sendBroadcast(msgTemplate);
    }

    public void sendBroadcast(Message msg){
        byte[] bytes = msg.constructMessageString().getBytes();
        DatagramPacket pkt = new DatagramPacket(bytes, bytes.length, Conf.MULTICAST_GROUP_ADDRESS, Conf.UDP_PORT);
        try {
            multicastSocket.send(pkt);
        } catch (IOException e) {
            System.out.println("ERROR Failed to send broadcast message; IO exception occurred");
        }

        //runningConnections.forEach((s, client) -> {
        //    client.sendMessage(msg.constructMessageString());
        //});
    }

    class MyThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()){
                try {
                    ClientWorker cl = new ClientWorker(serverSocket.accept());
                    Thread thread = new Thread(cl);
                    thread.start();
                    cl.askForCredentials();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

