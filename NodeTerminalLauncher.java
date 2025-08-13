import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class NodeTerminalLauncher {

    private void compileAllSources(String workingDir) {
        try {
            String findCommand = String.format("find %s -name \"*.java\"", workingDir);

            Process findProcess = new ProcessBuilder("bash", "-c", findCommand).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(findProcess.getInputStream()));
            StringBuilder sourcesList = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sourcesList.append(line).append(" ");
            }
            findProcess.waitFor();

            if (sourcesList.length() == 0) {
                System.exit(1);
            }

           String compileCommand = String.format(
                "cd %s && javac -d .out %s",
                workingDir,
                sourcesList.toString()
            );

            Process compileProcess = new ProcessBuilder("bash", "-c", compileCommand)
                    .inheritIO()
                    .start();

            int exitCode = compileProcess.waitFor();
            if (exitCode != 0) {
                System.exit(1);
            }

        } catch (IOException | InterruptedException e) {
                System.out.println(e);
        }
        
    }

    public void materialise(String csvFile, String workingDir) {

        compileAllSources(workingDir);

        List<NodeConfigLoader.NodeConfig> configs = NodeConfigLoader.loadConfigs(csvFile);

        File pubKeyFileGen= new File("validators/" + "Genesis" + "/public.key");
        File privKeyFileGen = new File("validators/" + "Genesis" + "/private.key");
        if (!pubKeyFileGen.exists() || !privKeyFileGen.exists()) KeyPairGeneratorTool.generateAndStoreKeys("Genesis");

        for (NodeConfigLoader.NodeConfig cfg : configs) {
            File pubKeyFile = new File("validators/" + cfg.id + "/public.key");
            File privKeyFile = new File("validators/" + cfg.id + "/private.key");
            if (!pubKeyFile.exists() || !privKeyFile.exists()) KeyPairGeneratorTool.generateAndStoreKeys(cfg.id);
        }

        for (NodeConfigLoader.NodeConfig cfg : configs) {
            StringBuilder peers = new StringBuilder();
            for (int i = 0; i < cfg.peerPorts.length; i++) {
                peers.append(cfg.peerPorts[i]);
                if (i < cfg.peerPorts.length - 1) peers.append(",");
            }

            String nodeCommand = String.format(
                "java -cp %s/.out Node %s %s %d %s %d %d",
                workingDir,
                cfg.id,
                cfg.model,
                cfg.myPort,
                peers,
                cfg.faultyCount,
                cfg.timeoutSeconds
            );

            String appleScriptCommand = String.format(
                "osascript -e 'tell application \"Terminal\" to do script \"cd %s; echo -ne \\\"\\\\033]0;%s\\\\007\\\" && %s\"'",
                workingDir,
                cfg.id,
                nodeCommand
            );

            try {
                new ProcessBuilder("zsh", "-c", appleScriptCommand).start();
                Thread.sleep(500); 
            } catch (IOException | InterruptedException e) {
                System.out.println(e);
            }
        }
    }
}