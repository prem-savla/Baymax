import java.io.*;
import java.security.*;
import java.util.Base64;

public class KeyPairGeneratorTool {

    public static void generateAndStoreKeys(String validatorId) {
        try{
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048); // 2048-bit RSA
            KeyPair pair = keyGen.generateKeyPair();

            // Save keys to file
            File dir = new File("validators/" + validatorId);
            dir.mkdirs();

            try (FileWriter pubOut = new FileWriter(new File(dir, "public.key"));
                FileWriter privOut = new FileWriter(new File(dir, "private.key"))) {
                pubOut.write(encodeKey(pair.getPublic()));
                privOut.write(encodeKey(pair.getPrivate()));
            }catch(Exception e){
                System.out.println(e);
            }
        }catch(Exception e){
            System.out.println(e);
        }
        

    }
    
    private static String encodeKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}