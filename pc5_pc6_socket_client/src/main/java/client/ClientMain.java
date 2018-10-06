package client;

import javax.swing.*;

public class ClientMain {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Client");
        ClientForm clientForm = new ClientForm();
        frame.setContentPane(clientForm.root);
        //frame.addWindowListener(clientForm);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(false);
        frame.pack();
        frame.setVisible(true);
    }
}
