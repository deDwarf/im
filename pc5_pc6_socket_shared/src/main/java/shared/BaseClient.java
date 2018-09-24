package shared;

import shared.msg.Message;

import java.io.*;
import java.net.Socket;

public abstract class BaseClient implements Runnable {
    protected String username;
    protected Socket mySocket;
    protected boolean timeToStop;

    protected OutputStreamWriter osw;
    protected BufferedReader br;

    public BaseClient(Socket socket){
        this.mySocket = socket;
        timeToStop  = false;
        try {
            osw = new OutputStreamWriter(new BufferedOutputStream(mySocket.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (!timeToStop){
            try {
                String input = readToTheEnd();
                handleInput(input);
            } catch (IOException e) {
                onIOExceptionInRun(e);
            }
        }
    }

    protected void onIOExceptionInRun(Exception e){}

    public void close(String reason){
        this.timeToStop = true;
    }

    public void close() {
        this.timeToStop = true;
    }

    abstract protected void handleInput(String string);

    public void sendMessage(String message){
        try {
            osw.write(message);
            osw.flush();
        } catch (IOException e) {
            try {
                osw.close();
                br.close();
                mySocket.close();
            } catch (IOException e1) { }
        }
    }

    protected String readToTheEnd() throws IOException {
        StringBuilder bldr = new StringBuilder();
        String str;
        while (!Message.MESSAGE_END.equals(str = br.readLine())) {
            bldr.append(str).append("\r\n");
        }
        bldr.append(str);
        return bldr.toString();
    }

    public boolean isConnected(){
        if (mySocket.isClosed()){
            this.close();
            return false;
        }
        return true;
    }

    public Socket getSocket() {
        return mySocket;
    }

    public String getUsername() {
        return username;
    }
}
