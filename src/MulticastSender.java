
import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastSender {

    private MulticastSocket socket;
    private InetAddress address;
    private int port = 20480;

    public MulticastSender() throws IOException {
        address = InetAddress.getByName("239.0.1.2");
        socket = new MulticastSocket();
    }

    public void close() throws IOException{
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public void sendMessage(String name, String msg) throws IOException{
        String dataToSend = name + ": " + msg;
        byte[] data = dataToSend.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

}