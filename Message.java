import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
            
    private final String blockId;         
    private final Type type;        
    private final Block block;  
    private final String senderId;            

    public enum Type {
        PROPOSE,
        VOTE
    }

    public Message(Type type, String senderId, Block block) {
        this.blockId =block.getHash();
        this.type = type;
        this.block = block;
        this.senderId = senderId;
    }

    public String getBlockId() { return blockId; }

    public Type getType() { return type; }

    public Block getBlock() { return block; }

     public String getSenderId() { return senderId; }

}