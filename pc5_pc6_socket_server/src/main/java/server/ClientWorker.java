package server;

import shared.*;
import shared.msg.ChatMessage;
import shared.msg.LoginRequestMessage;
import shared.msg.Message;
import shared.msg.ServiceMessage;

import java.io.*;
import java.net.Socket;
import java.util.List;

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
            serverMessageTemplate.setMessage("Unsupported message format");
            this.sendMessage(serverMessageTemplate);
            return;
        }
        if (msg instanceof LoginRequestMessage){
            if (isAuthorized){
                serverMessageTemplate.setMessage("Illegal state. You are already logged in");
                this.sendMessage(serverMessageTemplate);
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
        if (msg instanceof ServiceMessage) {
            ServiceMessage.Type msgType = ((ServiceMessage) msg).getMsgType();
            if (msgType == ServiceMessage.Type.GET_ONLINE_LIST) {
                Iterable<String> users = Server.getInstance().getOnlineUsers();
                serverMessageTemplate.setMsgType(msgType);
                serverMessageTemplate.setMessage(String.join(",", users));
                sendMessage(serverMessageTemplate);
            }
            else if (msgType == ServiceMessage.Type.GET_USER_LIST) {
                Iterable<String> users = Server.getInstance().getAllUsers();
                serverMessageTemplate.setMsgType(msgType);
                serverMessageTemplate.setMessage(String.join(",", users));
                sendMessage(serverMessageTemplate);
            }
            else if (msgType == ServiceMessage.Type.GET_UNDELIVERED_MESSAGES) {
                List<ChatMessage> msgs = Server.getInstance().getDb().getMessagesUndeliveredToUser(username);
                msgs.forEach(this::sendMessage);
            }
        }

        // at this point we know this should be a ChatMessage (if not - just ignore it)
        if (!(msg instanceof ChatMessage)){
            return;
        }
        Server.getInstance().routeMessageAmongUsers((ChatMessage) msg);
    }

    @Override
    protected void onIOExceptionInRun(Exception e) {
        close("Error while reading data from network");
    }

    @Override
    public void close(String reason){
        super.close();
        this.serverMessageTemplate.setMsgType(ServiceMessage.Type.CONNECTION_CLOSED);
        this.serverMessageTemplate.setMessage(reason);
        sendMessage(serverMessageTemplate);
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
        this.sendMessage(serverMessageTemplate);
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
