package client;

import org.apache.commons.lang3.ArrayUtils;
import shared.conf.Conf;
import shared.msg.*;
import sun.java2d.pipe.SpanShapeRenderer;

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

public class ClientForm extends JFrame implements WindowListener{
    private final String BROADCAST_NAME = "broadcast";

    public JPanel root;
    private JButton connect_btn;
    private JButton refresh_ulist_btn;
    private JTextField login_fld;
    private JPasswordField pwd_fld;
    private JTable userList;
    private JTextPane inputPane;
    private JTextPane outputPane;
    private JButton send_btn;
    private JScrollPane outputPaneScrollParent;

    private Client client;
    private Thread clientThread;
    private MessageHistoryManager manager;
    private String currentInterlocutor = BROADCAST_NAME; // default tab

    ClientForm() {
        super();
        prepareUserTableModel();
        manager = new MessageHistoryManager();
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
                Message msg  = new ServiceMessage(ServiceMessage.Type.UPDATE_ONLINE_LIST);
                client.sendMessage(msg);
            }
        });

        send_btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String body = inputPane.getText();
                String from = client.getUsername();
                String to = BROADCAST_NAME.equals(currentInterlocutor) ? null : currentInterlocutor;
                Message msg = new ChatMessage(from, to, body);
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
        //updateUserTable(new String[]{});
    }

    Client.ActionHandler handler = msg -> {
        if (msg instanceof ChatMessage) {
            String from = ((ChatMessage)msg).getFrom();
            addMessage(from, ((ChatMessage)msg).getTo(), ((ChatMessage)msg).getMessageBody(), msg.getTimestamp());
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
                Message msge  = new ServiceMessage(ServiceMessage.Type.UPDATE_ONLINE_LIST);
                client.sendMessage(msge);
            }

            else if (msgType == ServiceMessage.Type.AUTH_FAIL
                    || msgType == ServiceMessage.Type.CONNECTION_CLOSED) {
                connect_btn.setText("Connect");
                this.close();
                String reason = ((ServiceMessage) msg).getMessage();
                String message = "Connection closed." +
                        ((reason != null) ? " For reason: " + reason: "");
                addError(Message.SERVICE_NAME, message, msg.getTimestamp());
            }

            else if (msgType == ServiceMessage.Type.UPDATE_ONLINE_LIST){
                String[] msgs = ((ServiceMessage) msg).getMessage().split(",");
                updateUserTable(msgs);
            }
        }
    };

    private void addMessage(String from, String to, String msg, long timestamp) {
        String tabname;
        if (to == null && BROADCAST_NAME.equals(currentInterlocutor)) {
            tabname = BROADCAST_NAME;
            this.addTextToPane(1, from, msg, timestamp);
        }
        else if (to == null) {
            tabname = BROADCAST_NAME;
            increaseUnreadCount(BROADCAST_NAME);
        }
        else if (from.equals(currentInterlocutor)){
            tabname = from;
            this.addTextToPane(1, from, msg, timestamp);
        }
        else {
            tabname = from;
            increaseUnreadCount(from);
        }
        manager.add(tabname, formatText(1, from, msg, timestamp));
    }

    private void addMyMessage(String msg, long timestamp){
        manager.add(currentInterlocutor, formatText(1, "Me", msg, timestamp));
        this.addTextToPane(1, "Me", msg, timestamp);
    }

    private void addError(String from, String msg, long timestamp) {
        manager.add(currentInterlocutor, formatText(2, from, msg, timestamp));
        this.addTextToPane(2, from, msg, timestamp);
    }

    private void addInfo(String from, String msg, long timestamp) {
        manager.add(currentInterlocutor, formatText(3, from, msg, timestamp));
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

    // 1 - message
    // 2 - error
    // 3 - info
    public void addTextToPane(int type, String from, String msg, long timestamp){
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

    public void  displayErrorMsg(String title, String text) {
        JOptionPane.showMessageDialog(new JFrame(), text, title,
                JOptionPane.ERROR_MESSAGE);
    }

    private void close(){
        try {
            client.close();
            //clientThread.join();
            clientThread = null;
            System.out.println("Done!");
        } catch (Exception ex){
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {
        close();
    }

    //// --------------- TABLE --------------------------- ////
    private void increaseUnreadCount(String username) {
        DefaultTableModel model = (DefaultTableModel) userList.getModel();
        for (int i = 0; i < model.getRowCount(); i++){
            if (model.getValueAt(i, 0).equals(username)) {
                Object obj = model.getValueAt(i, 1);
                model.setValueAt(Integer.parseInt(obj.toString()) + 1, i, 1);
            }
        }
    }

    // will loose "unread count", but who cares
    private void updateUserTable(String[] users){
        Map<String, Integer> userCountMap = new HashMap<>();

        users = ArrayUtils.add(users, 0, BROADCAST_NAME);
        users = ArrayUtils.removeElement(users, client.getUsername());
        DefaultTableModel model = (DefaultTableModel) userList.getModel();
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            userCountMap.put(
                    (String)model.getValueAt(i, 0),
                    (Integer)model.getValueAt(i, 1));
            model.removeRow(i);
        }
        for (int i = 0; i < users.length; i++) {
            int savedCount = userCountMap.get(users[i]) == null ? 0: userCountMap.get(users[i]);
            model.addRow(new Object[]{ users[i], savedCount});
            if (users[i].equals(currentInterlocutor)){
                userList.changeSelection(i,0, false, true);
            }
        }
        currentInterlocutor = (String)model.getValueAt(0, 0);
    }

    private void prepareUserTableModel(){
        DefaultTableModel tableModel = (DefaultTableModel) userList.getModel();
        tableModel.addColumn("Username");
        tableModel.addColumn("Unread messages");
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        userList.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = userList.getSelectedRow();
            if (selectedRow < 0) {
                return;
            }
            userList.setValueAt(0, selectedRow, 1);
            String username = (String)userList.getModel().getValueAt(selectedRow, 0);
            if (username.equals(currentInterlocutor)){
                return;
            }
            currentInterlocutor = username;
            setTextToPane(manager.get(username));
        });
    }

    /// ------------------- NOT IMPLEMENTED ------------------- ////
    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
