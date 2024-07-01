import java.io.IOException;
import java.net.*;

public class MulticastCollector {

    private MessageListener listener;

    public MulticastCollector(MessageListener listener) throws IOException {
        this.listener = listener;
        int port = 20480;
        String ip = "239.0.1.2";
        InetAddress address = InetAddress.getByName(ip);

        InetSocketAddress group = new InetSocketAddress(address, port);
        NetworkInterface netIf = NetworkInterface.getByName("en0");

        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(group, netIf);

        byte[] data = new byte[256];

        try {
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket packet = new DatagramPacket(data, data.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                if (listener != null)
                    listener.onMessageReceived(msg);
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null){
                socket.close();
            }
        }
    }
}