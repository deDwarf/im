package client;

import org.apache.commons.lang3.ArrayUtils;
import shared.conf.Conf;
import shared.msg.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.html.HTMLDocument;
import java.awt.event.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientForm extends JFrame {
    private final String BROADCAST_NAME = "broadcast";

    public JPanel root;
    private JButton connect_btn;
    private JButton refresh_ulist_btn;
    private JTextField login_fld;
    private JPasswordField pwd_fld;
    private JTable contactsTable;
    private JTextPane inputPane;
    private JTextPane outputPane;
    private JButton send_btn;
    private JScrollPane outputPaneScrollParent;
    private JButton subscribeBtn;

    private Client client;
    private Thread clientThread;
    private MessageHistoryManager histManager;
    private String currentInterlocutor = BROADCAST_NAME; // default tab

    ClientForm() {
        super();
        prepareUserTableModel();
        histManager = new MessageHistoryManager();
        connect_btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                SwingUtilities.invokeLater(() -> {
                    if (client != null) {
                        close();
                        connect_btn.setText("Connect");
                    }
                    try {
                        InetAddress host_add = InetAddress.getByName(Conf.HOST_NAME);
                        Socket socket = new Socket(host_add, Conf.TCP_PORT);
                        client = new Client(socket);
                        client.setOnMessageReceived(handler);
                        clientThread = new Thread(client);
                        clientThread.start();

                        String username = login_fld.getText();
                        String password = new String(pwd_fld.getPassword());
                        LoginRequestMessage msg = new LoginRequestMessage(username, password);
                        client.sendMessage(msg);
                    } catch (UnknownHostException ex) {
                        addError(Message.SERVICE_NAME, "Unable to resolve host: " +
                                Conf.HOST_NAME, new Date().getTime());
                    } catch (IOException e1) {
                        addError(Message.SERVICE_NAME, "Failed to connect to the socket:" +
                                e1.getMessage(), new Date().getTime());
                    }
                });
            }
        });
        refresh_ulist_btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (client == null) {
                    displayErrorMsg("Unauthorized", "You need to login to get the online list");
                    return;
                }
                ServiceMessage msg  = new ServiceMessage(ServiceMessage.Type.GET_USER_LIST);
                client.sendMessage(msg);
            }
        });

        send_btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (client == null) {
                    displayErrorMsg("Unauthorized", "You need to login to send messages");
                    return;
                }
                String body = inputPane.getText();
                String from = client.getName();
                String destChat = client.getName();
                String to = BROADCAST_NAME.equals(currentInterlocutor) ? null : currentInterlocutor;
                Message msg = new ChatMessage(from, to, destChat, body);
                client.sendMessage(msg);
                inputPane.setText("");
                addMyMessage(body, msg.getTimestamp());
            }
        });

        inputPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER){ return; }
                super.keyTyped(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER){
                    for (MouseListener mouseListener : send_btn.getMouseListeners()) {
                        mouseListener.mouseClicked(null);
                    }
                }
            }
        });

        subscribeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (subscribeBtn.getText().equals("Subscribe")) {
                    ServiceMessage outMsg = new ServiceMessage(ServiceMessage.Type.SUBSCRIBE_GROUP);
                    outMsg.setMessage(currentInterlocutor);
                    client.sendMessage(outMsg);
                }
                else {
                    ServiceMessage outMsg = new ServiceMessage(ServiceMessage.Type.UNSUBSCRIBE_GROUP);
                    outMsg.setMessage(currentInterlocutor);
                    client.sendMessage(outMsg);
                }
            }
        });
        //updateUsersTableWithUsers(new String[]{});
    }

    Client.ActionHandler handler = msg -> {
        if (msg instanceof ChatMessage) {
            String from = ((ChatMessage)msg).getFrom();
            addMessage(
                    from,
                    ((ChatMessage)msg).getTo(),
                    ((ChatMessage) msg).getDestinationChat(),
                    ((ChatMessage)msg).getMessageBody(),
                    msg.getTimestamp()
            );
        }
        if (msg instanceof ServiceMessage) {
            ServiceMessage.Type msgType = ((ServiceMessage) msg).getMsgType();

            if (msgType == ServiceMessage.Type.REQUIRE_AUTH) {
                addError(Message.SERVICE_NAME, "Server requested you to login", msg.getTimestamp());
            }
            else if (msgType == ServiceMessage.Type.INFO || msgType == ServiceMessage.Type.ERROR) {
                addInfo(Message.SERVICE_NAME, ((ServiceMessage) msg).getMessage(), msg.getTimestamp());
            }
            else if (msgType == ServiceMessage.Type.AUTH_SUCCESS) {
                connect_btn.setText("Connected");

                client.setUsername(((ServiceMessage) msg).getMessage());
                addInfo(Message.SERVICE_NAME, "Login successful!", msg.getTimestamp());

                // update table
                Message msge  = new ServiceMessage(ServiceMessage.Type.GET_USER_LIST);
                client.sendMessage(msge);
            }
            else if (msgType == ServiceMessage.Type.AUTH_FAIL || msgType == ServiceMessage.Type.CONNECTION_CLOSED) {
                connect_btn.setText("Connect");
                this.close();
                String reason = ((ServiceMessage) msg).getMessage();
                String message = "Connection closed." +
                        ((reason != null) ? " For reason: " + reason: "");
                addError(Message.SERVICE_NAME, message, msg.getTimestamp());
            }
            else if (msgType == ServiceMessage.Type.GET_ONLINE_LIST){
                String[] msgs = ((ServiceMessage) msg).getMessage().split(",");
                updateUserListWithOnlineStatus(msgs);
            }
            else if (msgType == ServiceMessage.Type.GET_GROUP_LIST) {
                String[] msgs = ((ServiceMessage) msg).getMessage().split(",");
                updateUsersTableWithGroups(msgs);
            }
            else if (msgType == ServiceMessage.Type.GET_USER_LIST){
                String[] msge = ((ServiceMessage) msg).getMessage().split(",");
                updateUsersTableWithUsers(msge);

                ServiceMessage outMsg = new ServiceMessage(ServiceMessage.Type.GET_ONLINE_LIST);
                client.sendMessage(outMsg);

                outMsg.setMsgType(ServiceMessage.Type.GET_UNDELIVERED_MESSAGES);
                client.sendMessage(outMsg);

                outMsg.setMsgType(ServiceMessage.Type.GET_GROUP_LIST);
                client.sendMessage(outMsg);
            }
            else if (msgType == ServiceMessage.Type.SUBSCRIBE_GROUP) {
                this.updateGroupSubscribeStatus(((ServiceMessage) msg).getMessage(), true);
            }
            else if (msgType == ServiceMessage.Type.UNSUBSCRIBE_GROUP) {
                this.updateGroupSubscribeStatus(((ServiceMessage) msg).getMessage(), false);
            }
        }
    };

    public void displayErrorMsg(String title, String text) {
        JOptionPane.showMessageDialog(new JFrame(), text, title,
                JOptionPane.ERROR_MESSAGE);
    }

    private void close(){
        try {
            client.close();
            clientThread = null;
            System.out.println("Done!");
        } catch (Exception ex){
        }
    }

    private void addMessage(String from, String to, String destChat, String msg, long timestamp) {
        String tabname;
        if (to == null && BROADCAST_NAME.equals(currentInterlocutor)) {
            tabname = BROADCAST_NAME;
            this.addTextToPane(1, from, msg, timestamp);
        }
        else if (to == null) {
            tabname = BROADCAST_NAME;
            increaseUnreadCount(BROADCAST_NAME);
        }
        else if (destChat.equals(currentInterlocutor)){
            tabname = destChat;
            this.addTextToPane(1, from, msg, timestamp);
        }
        else {
            tabname = destChat;
            increaseUnreadCount(destChat);
        }
        histManager.add(tabname, formatText(1, from, msg, timestamp));
    }

    private void addMyMessage(String msg, long timestamp){
        histManager.add(currentInterlocutor, formatText(1, "Me", msg, timestamp));
        this.addTextToPane(1, "Me", msg, timestamp);
    }

    private void addError(String from, String msg, long timestamp) {
        histManager.add(currentInterlocutor, formatText(2, from, msg, timestamp));
        this.addTextToPane(2, from, msg, timestamp);
    }

    private void addInfo(String from, String msg, long timestamp) {
        histManager.add(currentInterlocutor, formatText(3, from, msg, timestamp));
        this.addTextToPane( 3, from, msg, timestamp);
    }

    private String formatText(int type, String from, String msg, long timestamp){
        final SimpleDateFormat df = new SimpleDateFormat("hh:mm");
        final String[] patterns = new String[] {
                "<b>%s    %s: </b><br>%s<br>", // message pattern
                "<font color='red'><b>%s    %s [ERROR]: </b>%s<br></font>", // error pattern
                "<font color='#31708f'><b>%s    %s [INFO]: </b>%s<br></font>"// info pattern
        };
        return String.format(patterns[type-1], df.format(new Date(timestamp)), from, msg);
    }

    public void addTextToPane(int type, String from, String msg, long timestamp){
        addTextToPane(type, from, from, msg, timestamp);
    }

    // 1 - message
    // 2 - error
    // 3 - info
    public void addTextToPane(int type, String from, String destChat, String msg, long timestamp){
        SwingUtilities.invokeLater(()-> {
                HTMLDocument doc = (HTMLDocument) outputPane.getStyledDocument();
                try {
                    doc.insertAfterEnd(
                            doc.getCharacterElement(doc.getLength()),
                            formatText(type, from, msg, timestamp));
                } catch (Exception e){
                    e.printStackTrace();
                }
                final JScrollBar scroll = outputPaneScrollParent.getVerticalScrollBar();
                scroll.setValue(scroll.getMaximum());
            }
        );
    }

    public void setTextToPane(List<String> history){
        SwingUtilities.invokeLater(()-> {
                outputPane.setText("");
                if (history == null) {
                    return;
                }
                HTMLDocument doc = (HTMLDocument) outputPane.getStyledDocument();
                try {
                    for (String s : history) {
                        doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), s);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                final JScrollBar scroll = outputPaneScrollParent.getVerticalScrollBar();
                scroll.setValue(scroll.getMaximum());
            }
        );
    }

    //// --------------- TABLE --------------------------- ////
    private void increaseUnreadCount(String username) {
        DefaultTableModel model = (DefaultTableModel) contactsTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++){
            if (model.getValueAt(i, 2).equals(username)) { // Name
                Object obj = model.getValueAt(i, 3); // Unread
                model.setValueAt(Integer.parseInt(obj.toString()) + 1, i, 3); // Unread
            }
        }
    }


    private void updateUserListWithOnlineStatus(String[] usersOnline) {
        DefaultTableModel model = (DefaultTableModel) contactsTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (ArrayUtils.contains(usersOnline, contactsTable.getValueAt(i, 2))) { // Name
                contactsTable.setValueAt("On", i, 0);
            }
            else {
                contactsTable.setValueAt("Off", i, 0);
            }
        }
    }

    private void updateGroupSubscribeStatus(String groupName, boolean status) {
        DefaultTableModel model = (DefaultTableModel) contactsTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (groupName.equals(contactsTable.getValueAt(i, 2))) { // Name
                if (status) {
                    contactsTable.setValueAt("Subscribed", i, 0);
                    if (currentInterlocutor.equals(groupName)) {
                        subscribeBtn.setText("Unsubscribe");
                    }
                }
                else {
                    contactsTable.setValueAt("Unsubscribed", i, 0);
                    if (currentInterlocutor.equals(groupName)) {
                        subscribeBtn.setText("Subscribe");
                    }
                }
            }
        }
    }

    private void updateUsersTableWithUsers(String[] users){
        users = ArrayUtils.add(users, 1, BROADCAST_NAME);
        users = ArrayUtils.removeElement(users, client.getName());

        Map<String, Integer> userCountMap = new HashMap<>();
        DefaultTableModel model = (DefaultTableModel) contactsTable.getModel();
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            if (contactsTable.getValueAt(i, 1) == "User") { // Type column
                userCountMap.put(
                        (String)model.getValueAt(i, 2),
                        (Integer)model.getValueAt(i, 3));
                model.removeRow(i);
            }
        }
        for (int i = 0; i < users.length; i++) {
            int savedCount = userCountMap.get(users[i]) == null ? 0: userCountMap.get(users[i]);

            model.addRow(new Object[]{ "Off", "User", users[i], savedCount});
            if (users[i].equals(currentInterlocutor)){
                contactsTable.changeSelection(i,i, false, true);
            }
        }
    }

    private void updateUsersTableWithGroups(String[] groups){
        Map<String, Integer> groupToUnreadMap = new HashMap<>();
        Map<String, String> groupToStatusMap = new HashMap<>();
        DefaultTableModel model = (DefaultTableModel) contactsTable.getModel();
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            if (contactsTable.getValueAt(i, 1) == "Group") { // Type column
                groupToUnreadMap.put(
                        (String)model.getValueAt(i, 2),
                        (Integer)model.getValueAt(i, 3));
                groupToStatusMap.put(
                        (String)model.getValueAt(i, 2),
                        (String)model.getValueAt(i, 1));
                model.removeRow(i);
            }
        }
        for (int i = 0; i < groups.length; i++) {
            int savedCount = groupToUnreadMap.get(groups[i]) == null
                    ? 0: groupToUnreadMap.get(groups[i]);
            String savedStatus = groupToStatusMap.get(groups[i]) == null
                    ? "Unsubscribed": groupToStatusMap.get(groups[i]);

            model.addRow(new Object[]{ savedStatus, "Group", groups[i], savedCount });
            if (groups[i].equals(currentInterlocutor)){
                contactsTable.changeSelection(i, i, false, true);
            }
        }
    }

    private void prepareUserTableModel(){
        DefaultTableModel tableModel = (DefaultTableModel) contactsTable.getModel();
        tableModel.addColumn("Status");
        tableModel.addColumn("Type");
        tableModel.addColumn("Username");
        tableModel.addColumn("Unread");

        contactsTable.getColumnModel().getColumn(0).setPreferredWidth(1);

        contactsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contactsTable.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = contactsTable.getSelectedRow();
            if (selectedRow < 0) {
                return;
            }
            String chosenInterlocutor = (String) contactsTable.getModel().getValueAt(selectedRow, 2);
            if (chosenInterlocutor.equals(currentInterlocutor)){
                return;
            }

            contactsTable.setValueAt(0, selectedRow, 3);
            currentInterlocutor = chosenInterlocutor;
            setTextToPane(histManager.get(chosenInterlocutor));

            if (contactsTable.getValueAt(selectedRow, 1).equals("Group")) {
                this.subscribeBtn.setVisible(true);
                if (contactsTable.getValueAt(selectedRow, 0).equals("Subscribed")) {
                    this.subscribeBtn.setText("Unsubscribe");
                }
                else {
                    this.subscribeBtn.setText("Subscribe");
                }
            } else {
                this.subscribeBtn.setVisible(false);
            }
        });
    }
}
