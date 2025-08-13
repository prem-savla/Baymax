import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NodeConfigLoader {

    public static class NodeConfig {
        public String id;
        public int myPort;
        public String model;
        public int timeoutSeconds;
        public int faultyCount;
        public int[] peerPorts;

        public NodeConfig(String id, int myPort, String model, int timeoutSeconds, int faultyCount, int[] peerPorts) {
            this.id = id;
            this.myPort = myPort;
            this.model = model;
            this.timeoutSeconds = timeoutSeconds;
            this.faultyCount = faultyCount;
            this.peerPorts = peerPorts;
        }
    }

    public static List<NodeConfig> loadConfigs(String csvFile){
        List<NodeConfig> configs = new ArrayList<>();
        List<Integer> allPorts = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line = br.readLine(); 
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                allPorts.add(Integer.parseInt(parts[1].trim()));
            }
        }catch (IOException e) {
            System.out.println(e);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line = br.readLine(); 
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");

                String id = parts[0].trim();
                int myPort = Integer.parseInt(parts[1].trim());
                String model = parts[2].trim();
                int timeoutSeconds = Integer.parseInt(parts[3].trim());
                boolean faulty = Boolean.parseBoolean(parts[4].trim());

                int faultyCount = faulty ? 1 : 0;

                List<Integer> peerList = new ArrayList<>(allPorts);
                peerList.remove(Integer.valueOf(myPort));

                int[] peerPorts = peerList.stream().mapToInt(Integer::intValue).toArray();

                configs.add(new NodeConfig(id, myPort, model, timeoutSeconds, faultyCount, peerPorts));
            }
        }catch (IOException e) {
            System.out.println(e);
        }

        return configs;
    }
}