import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
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


    private void saveMembersToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("members.txt"))) {
            for (String member : members) {
                writer.write(member);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMembersFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("members.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                members.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    ChatGUI() {
        super("Chat Application");
        username = JOptionPane.showInputDialog(this, "Enter your name");
        if (!checkIfUsernameIsValid(username)) {
            System.exit(0);
        }

        setupGUI();
        loadMembersFromFile();

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
                saveMembersToFile();
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

    private void sendSystemMessage(String message) {
        try {
            sender.sendMessage("SYSTEM", message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendUserConnect(){
        updateMemberList(username, true);
        sendSystemMessage("CONNECTED: " + username);
    }


    private void sendUserDisconnect(){
        updateMemberList(username, false);
        sendSystemMessage("DISCONNECTED: " + username);
    }

    private void updateMemberList(String username, boolean add){
        boolean changed = false;
        if (add){
            changed = members.add(username);
        } else {
            changed = members.remove(username);
        }
        if(changed){
            saveMembersToFile();
            refreshMemberList();
        }
    }

    private void refreshMemberList(){

        memberArea.setText("");
        for (String user : members){
            memberArea.append(user + "\n");
        }
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
            } else if (msg.startsWith("REQUEST_MEMBERLIST")){
                broadcastUserListToRequester(msg.substring(17).trim());
            }
        });
    }

    private void broadcastUserListToRequester(String requesterUsername){
        String userList = "MEMBERLIST: " + String.join("\n", members);
        try {
            sender.sendMessage(requesterUsername, userList);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            updateMemberListView(message.substring(12).trim());
        } else if(message.startsWith("REQUEST_MEMBERLIST")){
            handleSystemMessage("REQUEST_MEMBERLIST: " + String.join("\n", members));
        } else {
            SwingUtilities.invokeLater(() -> {
                chatArea.append(message + "\n");
            });
        }

    }



}
