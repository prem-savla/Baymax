import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;

public class PortLink {
    private final int myPort;
    private final int[] peerPorts;
    private final LinkedBlockingQueue<Message> inbound; 

    public PortLink(int myPort, int[] peerPorts,LinkedBlockingQueue<Message> inbound) {
        this.myPort = myPort;
        this.peerPorts = peerPorts;
        this.inbound = inbound;

        new Thread(this::startServer).start();
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public LinkedBlockingQueue<Message> getQueue() { return inbound; }

    public int getPortCount(){ return peerPorts.length+1; }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(this.myPort)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleMessage(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private void handleMessage(Socket socket) {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            Object received = ois.readObject();
            if (received instanceof Message) {
                inbound.put((Message) received);
            }
        }catch (IOException | ClassNotFoundException e) {
        System.out.println(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void broadcastMessage(Message message) {
        for (int targetPort : peerPorts) {
            try (
                Socket socket = new Socket("localhost", targetPort);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())
            ) {
                oos.writeObject(message);
                oos.flush();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}