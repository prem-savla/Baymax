import java.security.*;
import java.util.*;

public class Blockchain {
    private final List<Block> chain = new ArrayList<>();
    private final String blockProposerId;
    private final PrivateKey thisPrivateKey;
    private final Map<String, PublicKey> validatorPublicKeys;
    private final String MODEL;
    private final int faulty ;

    public Blockchain(String blockProposerId, String MODEL, int faulty) {
        this.MODEL = MODEL;
        this.blockProposerId = blockProposerId;
        this.thisPrivateKey = CryptoUtils.loadPrivateKey("validators/" + blockProposerId + "/private.key");
        this.validatorPublicKeys = CryptoUtils.loadAllPublicKeys("validators");
        this.faulty = faulty;

        Block genesis = new Block(
            CryptoUtils.loadPrivateKey("validators/Genesis/private.key"),
            MODEL
        );
        chain.add(genesis);
    }
    
    public int getFaulty(){ return faulty; }

    public String getId(){ return blockProposerId; }

    public Block forgeBlock(String dataPath) {
        Block latest = getLatestBlock();
        int nextRound = latest.getIndex() + 1;
        String previousHash = latest.getHash();
        
        try {
            return new Block(nextRound, dataPath, MODEL, previousHash, blockProposerId, thisPrivateKey);
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }
    
    public void commitBlock(Block block) { chain.add(block); }

    public Block getLatestBlock() { return chain.get(chain.size() - 1); }

    public boolean isValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block curr = chain.get(i);
            Block prev = chain.get(i - 1);

            String recalculatedHash = CryptoUtils.calculateHash(curr.getPayload());
            if (!curr.getHash().equals(recalculatedHash)) return false;

            if (!curr.getPreviousHash().equals(prev.getHash())) return false;

            if (!curr.getModelHash().equals(prev.getModelHash())) return false;

            PublicKey proposerKey = validatorPublicKeys.get(curr.getBlockProposerId());
            if (proposerKey == null || !CryptoUtils.verifySignature(curr.getHash(), curr.getSignature(), proposerKey)) {
                return false;
            }
        }
        return true;
    }

    public boolean isValid(Block newBlock) {
        Block latest = getLatestBlock();

        if (!newBlock.getPreviousHash().equals(latest.getHash())) return false;

        if (!newBlock.getModelHash().equals(latest.getModelHash())) return false;

        String recalculatedHash = CryptoUtils.calculateHash(newBlock.getPayload());
        if (!newBlock.getHash().equals(recalculatedHash)) return false;

        PublicKey proposerKey = validatorPublicKeys.get(newBlock.getBlockProposerId());
        if (proposerKey == null || !CryptoUtils.verifySignature(newBlock.getHash(), newBlock.getSignature(), proposerKey)) {
            return false;
        }

        return true;
    }

    public void printChain() {
        for (Block block : chain) {
            System.out.println("Index: " + block.getIndex());
            System.out.println("Timestamp: " + block.getTimestamp());
            System.out.println("Data Hash: " + block.getDataHash());
            System.out.println("Model Hash: " + block.getModelHash());
            System.out.println("Proposer ID: " + block.getBlockProposerId());
            System.out.println("Hash: " + block.getHash());
            System.out.println("Previous Hash: " + block.getPreviousHash());
            System.out.println("Signature: " + block.getSignature());
            System.out.println("-----");
        }
    }

    
}