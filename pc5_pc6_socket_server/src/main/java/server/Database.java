package server;

import shared.msg.ChatMessage;
import shared.msg.Message;
import util.SQLUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Database {
    private static final String dbFilePath = "./db/users";
    private static Database INSTANCE = new Database();

    private Connection con;
    public static Database getInstance() {
        return INSTANCE;
    }

    private Database() {
        try {
            Class.forName("org.h2.Driver");
            con = DriverManager.getConnection("jdbc:h2:" + dbFilePath);
            SQLUtils.createUsersTableIfNotExists(con);
            SQLUtils.createMessagesTableIfNotExists(con);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Fatal. Unable to load DB driver class");
        } catch (SQLException e) {
            throw new RuntimeException("Fatal. Unable to establish DB connection");
        }
    }

    public boolean checkCredentials(String username, String password){
        return SQLUtils.checkCredentials(con, username, password);
    }

    public boolean registerUser(String username, String password){
        boolean userExists;
        try {
            userExists = SQLUtils.checkUserExists(con, username);
            if (userExists){
                return false;
            }
            SQLUtils.addUser(con, username, password);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public Map<String, String> getUserListWithCredentials(){
        return SQLUtils.getUserWithCredentialsList(con);
    }

    public Set<String> getUserList() {
        return SQLUtils.getUserWithCredentialsList(con).keySet();
    }

    public boolean checkUserExists(String username) {
        return SQLUtils.checkUserExists(con, username);
    }

    public void saveMessage(Message msg, boolean delivered) {
        if (msg instanceof ChatMessage) {
            SQLUtils.saveMessage(con, (ChatMessage)msg, delivered);
        }
    }

    public List<ChatMessage> getMessagesUndeliveredToUser(String username) {
        return SQLUtils.getUndeliveredMessagesForUser(con, username);
    }
}
