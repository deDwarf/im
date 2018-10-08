package shared;

import shared.msg.Message;

import java.io.*;
import java.net.Socket;

public abstract class BaseClient implements Runnable, IClient {
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

    abstract protected void handleInput(String string);

    protected void onIOExceptionInRun(Exception e){}

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

    @Override
    public void close(String reason){
        this.timeToStop = true;
    }

    @Override
    public void close() {
        this.timeToStop = true;
    }

    @Override
    public void sendMessage(Message message){
        try {
            osw.write(message.constructMessageString());
            osw.flush();
        } catch (IOException e) {
            try {
                osw.close();
                br.close();
                mySocket.close();
            } catch (IOException e1) { }
        }
    }

    @Override
    public String getAddress() {
        return mySocket.getInetAddress().toString();
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public boolean isConnected(){
        if (mySocket.isClosed()){
            this.close();
            return false;
        }
        return true;
    }

    private String readToTheEnd() throws IOException {
        StringBuilder bldr = new StringBuilder();
        String str;
        while (!Message.MESSAGE_END.equals(str = br.readLine())) {
            bldr.append(str).append("\r\n");
        }
        bldr.append(str);
        return bldr.toString();
    }
}
