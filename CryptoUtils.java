import java.io.InputStream;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class CryptoUtils {

    public static String calculateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) hexString.append(String.format("%02x", b));
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String hashFile(String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) return "";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(Paths.get(filePath))) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();

        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }

    public static String signData(String data, PrivateKey privateKey) {
        try {
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(data.getBytes());
            byte[] sigBytes = signer.sign();
            return Base64.getEncoder().encodeToString(sigBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifySignature(String data, String signature, PublicKey publicKey) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(data.getBytes());
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    public static PrivateKey loadPrivateKey(String path){
        try{
            byte[] keyBytes = Base64.getDecoder().decode(Files.readString(Path.of(path)));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        }catch(Exception e){
            System.out.println(e);
            return null;
        }
    }

    public static PublicKey loadPublicKey(String path){
        try{
            byte[] keyBytes = Base64.getDecoder().decode(Files.readString(Path.of(path)));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        }catch(Exception e){
            System.out.println(e);
            return null;
        }
    }

    public static Map<String, PublicKey> loadAllPublicKeys(String validatorsFolder) {
        try{
            Map<String, PublicKey> map = new HashMap<>();
            Files.list(Paths.get(validatorsFolder)).forEach(path -> {
                Path pubKeyPath = path.resolve("public.key");
                if (Files.exists(pubKeyPath)) {
                    try {
                        String id = path.getFileName().toString();
                        map.put(id, loadPublicKey(pubKeyPath.toString()));
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                }
            });
            return map;
        } catch(Exception e){
            System.out.println(e);
            return null;
        }
    }
}