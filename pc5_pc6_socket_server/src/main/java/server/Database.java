package server;

import util.SQLUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Map<String, String> listUsers(){
        return SQLUtils.getUserList(con);
    }

    public boolean checkUserExists(String username) {
        return SQLUtils.checkUserExists(con, username);
    }

}
