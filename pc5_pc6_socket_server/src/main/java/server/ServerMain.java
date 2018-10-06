package server;

import shared.msg.Message;
import shared.msg.ServiceMessage;

import java.util.Map;
import java.util.Scanner;

import static server.ServerMain.Options.*;

public class ServerMain {
    enum Options {
        START("start", "starts the server"),
        STOP("stop", "stops the server"),
        REGISTER("register", "request for uname/password and register a new user"),
        HELP("help", "print this message"),
        USER_LIST("userlist", "print list of available users"),
        EAVESDROP("eavesdrop", "eavesdrop on clients"),
        SEND_BROADCAST("broadcast", "broadcast message to all clients"),
        EXIT("exit", "terminate this program and exit");

        public final String name;
        public final String descrition;

        Options(String name, String description){
            this.name = name;
            this.descrition = description;
        }

        public static Options fromName(String name) {
            for (Options f : values()) {
                if (f.name.equalsIgnoreCase(name)) {
                    return f;
                }
            }
            return null;
        }

        public static String constructHelpMessage(){
            StringBuilder bldr = new StringBuilder();
            for (Options option: values()){
                bldr.append("* ").append(option.name).append(" - ").append(option.descrition).append("\r\n");
            }
            return bldr.toString();
        }
    }

    boolean stopFlag = false;
    Server server = Server.getInstance();
    Scanner scanner = new Scanner(System.in);
    String helpMessage = "+++ Yet Another Chat App +++\r\n" + constructHelpMessage();

    public static void main(String[] args) {
        ServerMain main = new ServerMain();
        System.out.println(main.helpMessage);
        while(!main.stopFlag){
            main.askForAction();
            String action = main.scanner.nextLine();
            main.handleAction(action);
        }
    }

    private void askForAction(){
        System.out.print(">> ");
    }

    private void handleAction(String action){
        Options opt = Options.fromName(action);
        if (opt == null){
            System.out.printf("'%s' option is not recognized\r\n", action);
            System.out.println(helpMessage);
            return;
        }
        switch (opt){
            case SEND_BROADCAST:
                String strMessage = scanner.nextLine();
                Message msg = new ServiceMessage(ServiceMessage.Type.INFO, strMessage);
                Server.getInstance().sendBroadcast(msg);
                break;
            case START:
                server.start();
                System.out.println("Server started");
                break;
            case STOP:
                server.stop();
                System.out.println("stopped");
                break;
            case HELP:
                System.out.println(helpMessage);
                break;
            case USER_LIST:
                Map<String, String> users = Database.getInstance().getUserListWithCredentials();
                if (users.isEmpty()){
                    System.out.println("No users are registered yet");
                    break;
                }
                System.out.println("User list:");
                final int[] counter = {1};
                users.forEach((s, s2) -> {
                    System.out.printf("%d) username: \"%s\", password: \"%s\"\r\n", counter[0]++, s, s2);
                });
                System.out.println();
                break;
            case EXIT:
                server.stop();
                this.stopFlag = true;
                break;
            case REGISTER:
                String username;
                String password;
                System.out.print("Enter username: ");
                username = scanner.nextLine();
                System.out.print("Enter password: ");
                password = scanner.nextLine();
                boolean result = Database.getInstance().registerUser(username, password);
                if (!result){
                    System.out.println();
                    System.out.println("Reg failed. An user with such name already exists");
                }
                else {
                    System.out.println();
                    System.out.println("Success!");
                }
                break;
        }
    }
}
