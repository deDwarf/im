package util;

import shared.msg.ChatMessage;
import shared.msg.Message;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class SQLUtils {

    private static final String USERS_TABLE_NAME = "Users";
    private static final String SELECT_USERS = String.format("SELECT * FROM %s where is_group = False", USERS_TABLE_NAME);
    private static final String SELECT_GROUPS = String.format("SELECT * FROM %s where is_group = True", USERS_TABLE_NAME);
    private static final String CREATE_TABLE_USERS =
            String.format("CREATE TABLE IF NOT EXISTS %s (username varchar(50), password varchar(50), is_group boolean)"
                    , USERS_TABLE_NAME);

    private static final String MESSAGES_TABLE_NAME = "Messages";
    private static final String CREATE_TABLE_MESSAGES =
            String.format("CREATE TABLE IF NOT EXISTS %s " +
                    "(from_usr varchar(50), to varchar(50), message varchar(500), delivered boolean)"
                    , MESSAGES_TABLE_NAME);


    public static Map<String, String> getUserWithCredentialsList(Connection con){
        Map<String, String> map = new HashMap<>();
        try {
            Statement statement = con.createStatement();
            ResultSet rs = statement.executeQuery(SELECT_USERS);
            while (rs.next()){
                String uname = rs.getString("username");
                String pwd = rs.getString("password");
                map.put(uname, pwd);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static Set<String> getGroups(Connection con) {
        Set<String> groups = new HashSet<>();
        try {
            Statement statement = con.createStatement();
            ResultSet rs = statement.executeQuery(SELECT_GROUPS);
            while (rs.next()){
                String groupName = rs.getString("username");
                groups.add(groupName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groups;
    }

    public static void addUser(Connection con, String username, String pwd) throws SQLException{
        Statement statement = con.createStatement();
        statement.executeUpdate(String.format(
                "INSERT INTO %s VALUES('%s', '%s', False)", USERS_TABLE_NAME, username, pwd));
    }

    public static void addGroup(Connection con, String groupName) {
        Statement statement = null;
        try {
            statement = con.createStatement();
            statement.executeUpdate(String.format(
                    "INSERT INTO %s VALUES('%s', 'N/A', True)", USERS_TABLE_NAME, groupName));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkCredentials(Connection con, String username, String pwd){
        Statement statement = null;
        try {
            statement = con.createStatement();
            ResultSet rs = statement.executeQuery(String.format(
                    "SELECT * FROM %s WHERE username='%s'", USERS_TABLE_NAME, username));
            if (!rs.next()){
                return false;
            }
            String bdPwd = rs.getString("password");
            return bdPwd != null && pwd != null && bdPwd.equals(pwd);
        } catch (SQLException e) {
            throw new RuntimeException("Fatal. Smth went wrong on DB level", e);
        }
    }

    public static boolean checkUserExists(Connection con, String username){
        Statement statement = null;
        try {
            statement = con.createStatement();
            ResultSet rs = statement.executeQuery(String.format(
                    "SELECT * FROM %s WHERE username='%s'", USERS_TABLE_NAME, username));
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Fatal. Smth went wrong on DB level", e);
        }
    }

    public static void createUsersTableIfNotExists(Connection con){
        Statement statement;
        try {
            statement = con.createStatement();
            statement.execute(CREATE_TABLE_USERS);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to create statement while creating Users table");
        }
    }


    public static void createMessagesTableIfNotExists(Connection con) {
        Statement statement;
        try {
            statement = con.createStatement();
            statement.execute(CREATE_TABLE_MESSAGES);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to create statement while creating Messages table", e);
        }
    }

    public static void saveMessage(Connection con, ChatMessage msg, boolean delivered) {
        try {
            Statement st = con.createStatement();
            st.execute(
                    String.format("INSERT INTO %s VALUES('%s', '%s', '%s', %s)"
                    , MESSAGES_TABLE_NAME, msg.getFrom(), msg.getTo(), msg.getMessageBody(), delivered)
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<ChatMessage> getUndeliveredMessagesForUser(Connection con, String username) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(String.format("SELECT * FROM %s WHERE to = '%s' and delivered = False"
                    , MESSAGES_TABLE_NAME, username));
            while (rs.next()) {
                ChatMessage msg = new ChatMessage(
                        rs.getString("from_usr"),
                        rs.getString("to"),
                        rs.getString("from_usr"),
                        rs.getString("message")
                );
                messages.add(msg);
            }
            st.execute(String.format("UPDATE %s SET delivered = True WHERE to = '%s'"
                    , MESSAGES_TABLE_NAME, username));
            return messages;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
