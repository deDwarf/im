package server;

import shared.*;
import shared.msg.ChatMessage;
import shared.msg.LoginRequestMessage;
import shared.msg.Message;
import shared.msg.ServiceMessage;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class ClientWorker extends BaseClient {
    private String username;
    private boolean isAuthorized;
    private ServiceMessage serverMessageTemplate = new ServiceMessage(ServiceMessage.Type.INFO);

    ClientWorker(Socket socket){
        super(socket);
        isAuthorized = false;
    }

    @Override
    protected void handleInput(String input){
        Message msg = Message.tryParse(input);
        if (msg == null) {
            serverMessageTemplate.setMessage("Unsupported format");
            this.sendMessage(serverMessageTemplate.getMessage());
            return;
        }
        if (msg instanceof LoginRequestMessage){
            if (isAuthorized){
                serverMessageTemplate.setMessage("Illegal state. You are already logged in");
                this.sendMessage(serverMessageTemplate.constructMessageString());
                return;
            }
            LoginRequestMessage lrmsg = (LoginRequestMessage)msg;
            Server.getInstance().onCredentialsReceived(lrmsg.getUsername(), lrmsg.getPassword(), this);
            return;
        }
        if (!isAuthorized) {
            this.askForCredentials();
            return;
        }
        if (msg instanceof ServiceMessage &&
                ((ServiceMessage) msg).getMsgType() == ServiceMessage.Type.UPDATE_ONLINE_LIST) {
            String[] users = Server.getInstance().getOnlineUsers();
            serverMessageTemplate.setMsgType(ServiceMessage.Type.UPDATE_ONLINE_LIST);
            serverMessageTemplate.setMessage(String.join(",", Arrays.asList(users)));
            sendMessage(serverMessageTemplate.constructMessageString());
        }
        if (!(msg instanceof ChatMessage)){
            return; // not supposed to happen
        }
        Server.getInstance().routeMessage((ChatMessage) msg);
    }

    @Override
    protected void onIOExceptionInRun(Exception e) {
        close("Unexpected data occurred while reading data from network");
    }

    @Override
    public void close(String reason){
        super.close();
        this.serverMessageTemplate.setMsgType(ServiceMessage.Type.CONNECTION_CLOSED);
        this.serverMessageTemplate.setMessage(reason);
        sendMessage(serverMessageTemplate.constructMessageString());
        this.close();
    }

    @Override
    public void close() {
        super.close();
        Server.getInstance().removeActiveConnection(this.username);
        try {
            osw.flush();
            osw.close();
            br.close();
            mySocket.close();
        } catch (IOException e) {
            // do nothing
        }
    }

    public void askForCredentials(){
        serverMessageTemplate.setMsgType(ServiceMessage.Type.REQUIRE_AUTH);
        serverMessageTemplate.setMessage(null);
        this.sendMessage(serverMessageTemplate.constructMessageString());
    }

    public void setAuthorized(String username){
        this.isAuthorized = true;
        this.username = username;
    }

    public Socket getSocket() {
        return mySocket;
    }

    /// ------ ///
}
