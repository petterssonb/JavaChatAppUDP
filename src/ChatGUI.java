import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;


public class ChatGUI extends JFrame implements ActionListener, MessageListener {
    private JPanel mainPanel;
    private JTextField messageInput;
    private JTextArea chatArea, memberArea;
    private JButton sendButton, disconnectButton;
    private String username;
    private MulticastSender sender;
    private Thread receiverThread;

    private Set<String> members = new LinkedHashSet<>();



    ChatGUI() {
        super("Chat Application");
        username = JOptionPane.showInputDialog(this, "Enter your name");
        if (!checkIfUsernameIsValid(username)) {
            System.exit(0);
        }

        try {
            sender = new MulticastSender();
            receiverThread = new Thread(() -> {
                try {
                    new MulticastCollector(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showInputDialog(this, "Error setting up network component");
        }
        receiverThread.start();

        setupGUI();
        sendUserConnect();


    }

    private void setupGUI(){
        mainPanel = new JPanel(new BorderLayout());

        chatArea = new JTextArea(20, 40);
        chatArea.setEditable(false);
        mainPanel.add(chatArea, BorderLayout.WEST);

        memberArea = new JTextArea(20, 20);
        memberArea.setEditable(false);
        mainPanel.add(memberArea, BorderLayout.EAST);

        disconnectButton = new JButton("Disconnect");
        JPanel disconnectPanel = new JPanel(new BorderLayout());
        disconnectPanel.add(disconnectButton, BorderLayout.CENTER);
        mainPanel.add(disconnectPanel, BorderLayout.NORTH);


        messageInput = new JTextField(30);
        sendButton = new JButton("Send");
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(messageInput);
        inputPanel.add(sendButton);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(this);
        disconnectButton.addActionListener(this);
        messageInput.addKeyListener(new KeyAdapter(){
            public void keyPressed(KeyEvent e){
                if(e.getKeyCode() == KeyEvent.VK_ENTER){
                    sendMessage();
                }
            }
        });

        this.setContentPane(mainPanel);
        this.setSize(800, 600);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sendUserDisconnect();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                System.exit(0);
            }
        });
        this.setVisible(true);
    }

    private void sendMessage(){
        String msg = messageInput.getText();
        if(!msg.isEmpty()){
            try {
                sender.sendMessage(username, msg);
                messageInput.setText("");
            } catch (IOException e){
                JOptionPane.showInputDialog(this, "Error sending message");

            }
        }
    }

    private boolean checkIfUsernameIsValid(String username){
        return username != null && !username.isEmpty();
    }

    private void sendSystemMessage(String message){
        try {
            sender.sendMessage("SYSTEM", message);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sendUserConnect(){
        updateMemberList(username, true);
        sendSystemMessage("CONNECTED: " + username);
        broadcastUserListUpdate();
    }

    private void sendUserDisconnect(){
        updateMemberList(username, false);
        sendSystemMessage("DISCONNECTED: " + username);
        broadcastUserListUpdate();
    }

    private void updateMemberList(String username, boolean add){
        if (add){
            members.add(username);
        } else {
            members.remove(username);
        }
        refreshMemberList();
    }

    private void refreshMemberList(){

        memberArea.setText("");
        for (String user : members){
            memberArea.append(user + "\n");
        }
    }

    private void requestMemberList(){
        sendSystemMessage("REQUEST_MEMBERLIST");
        refreshMemberList();
    }

    private void broadcastUserListUpdate(){
        String userList = ("MEMBERLIST: " + String.join("\n", members));
        sendSystemMessage(userList);
    }

    private void handleSystemMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (msg.startsWith("CONNECTED: ")) {
                String connectedUsername = msg.substring(11).trim();
                chatArea.append(connectedUsername + " has connected\n");
                updateMemberList(connectedUsername, true);
                broadcastUserListUpdate();
            } else if (msg.startsWith("DISCONNECTED: ")) {
                String disconnectedUsername = msg.substring(13).trim();
                chatArea.append(disconnectedUsername + " has disconnected\n");
                updateMemberList(disconnectedUsername, false);
                broadcastUserListUpdate();
            }
        });
    }

    private void updateMemberListView(String memberListString){
        SwingUtilities.invokeLater(() -> {
            String[] recievedMembers = memberListString.split("\n");
            members.clear();
            memberArea.setText("");
            for(String user : recievedMembers){
                members.add(user);
                memberArea.append(user + "\n");
            }
        });
    }


    @Override
    public void actionPerformed(ActionEvent e){
        if (e.getSource() == disconnectButton){
            receiverThread.interrupt();
            sendUserDisconnect();
            try{
                sender.close();
            } catch(IOException ex){
                ex.printStackTrace();
            }
            System.exit(0);
        } else if (e.getSource() == sendButton){
            sendMessage();
        }
    }

    @Override
    public void onMessageReceived(String message){
        if (message.startsWith("SYSTEM: ")) {
            handleSystemMessage(message.substring(7).trim());
        } else if(message.startsWith("MEMBERLIST: ")){
            SwingUtilities.invokeLater(() -> {
                memberArea.setText("");
                String[] receivedMembers = message.substring(11).trim().split("\n");
                members.clear();
                for(String user : receivedMembers){
                    members.add(user);
                    memberArea.append(user + "\n");
                }
            });

        } else if(message.startsWith("REQUEST_MEMBERLIST")){
            broadcastUserListUpdate();
        } else {
            SwingUtilities.invokeLater(() -> {
                chatArea.append(message + "\n");
            });
        }

    }



}
