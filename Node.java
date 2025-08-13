import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;

// the annoy function is just to get rid of the java warning ;)
public class Node {
    private final String Id;
    private final LinkedBlockingQueue<Message> inbound;
    private final Blockchain blockchain;
    private final PortLink network;
    private final MessageHandler handler;
    private final Scanner sc;

    private MessageHandler annoy() {return handler;}

    public Node(String Id, String model, int myPort, int[] peerPorts, int faulty, int timeoutSeconds){
        this.Id = Id;
        this.inbound = new LinkedBlockingQueue<>();
        this.blockchain = new Blockchain(Id, model, faulty);
        this.network = new PortLink(myPort, peerPorts, inbound);
        this.handler = new MessageHandler(network, blockchain, inbound, timeoutSeconds);
        this.sc = new Scanner(System.in);
        annoy();
    }

    private void blockPropose(String dataPath) {
        Block newBlock = blockchain.forgeBlock(dataPath);
        Message msg = new Message(Message.Type.PROPOSE, Id, newBlock);
        network.broadcastMessage(msg);
    }

    public void run(){
        while (true) {
            System.out.print("0: Print Chain | 1: Propose Block: ");
            int choice;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                continue;
            }

            switch (choice) {
                case 0:
                    blockchain.printChain();
                    break;
                case 1:
                    System.out.print("Enter model path: ");
                    String path = sc.nextLine();
                    blockPropose(path);
                    break;
                default:
                    System.out.println("Invalid option");
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("Usage: Node <id> <model> <myPort> <peerPortsCSV> <faultyCount> <timeoutSeconds>");
            return;
        }
        String id = args[0];
        String model = args[1];
        int myPort = Integer.parseInt(args[2]);
        String[] peerStr = args[3].split(",");
        int[] peers = new int[peerStr.length];
        for (int i = 0; i < peerStr.length; i++) {
            peers[i] = Integer.parseInt(peerStr[i]);
        }
        int faulty = Integer.parseInt(args[4]);
        int timeout = Integer.parseInt(args[5]);

        new Node(id, model, myPort, peers, faulty, timeout).run();
    }
}