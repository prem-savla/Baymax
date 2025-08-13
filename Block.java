import java.io.Serializable;
import java.security.PrivateKey;

public class Block implements Serializable{
    private static final long serialVersionUID = 1L;

    private final int index;
    private final long timestamp;
    private final String dataHash;
    private final String modelHash;
    private final String blockProposerId;
    private final String previousHash;
    private final String hash;
    private final String signature;

    
    public Block(int index, String dataPath, String model,
                 String previousHash, String blockProposerId, PrivateKey privateKey) throws Exception {
        this.index= index;
        this.timestamp = System.currentTimeMillis();
        this.previousHash = previousHash;
        this.blockProposerId = blockProposerId;
        this.dataHash = CryptoUtils.hashFile(dataPath);
        this.modelHash = CryptoUtils.calculateHash(model);
        this.hash = CryptoUtils.calculateHash(getPayload());
        this.signature = CryptoUtils.signData(this.hash, privateKey);
    }

    public Block(PrivateKey privateKey, String model){
        this.index= 0;
        this.timestamp = 0;
        this.previousHash = "0";
        this.blockProposerId = "Genesis";
        this.dataHash = "0";
        this.modelHash = CryptoUtils.calculateHash(model);
        this.hash = CryptoUtils.calculateHash(getPayload());
        this.signature =  CryptoUtils.signData(this.hash, privateKey);
    }

    public int getIndex() { return index; }

    public long getTimestamp() { return timestamp; }

    public String getDataHash() { return dataHash; }

    public String getModelHash() { return modelHash; }

    public String getBlockProposerId() { return blockProposerId; }

    public String getPreviousHash() { return previousHash; }

    public String getHash() { return hash; }

    public String getSignature() { return signature; }

    public String getPayload(){ return index+ timestamp + dataHash + modelHash + blockProposerId + previousHash; }
}