import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageHandler {
    private final String myId;
    private final BlockingQueue<Message> inbound;
    private final PortLink network;
    private final Blockchain blockchain;

    private final AtomicInteger round = new AtomicInteger(1);
    private final AtomicBoolean hasVoted = new AtomicBoolean(false);
    private final ConcurrentMap<String, Block> proposedBlocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> voteMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile ScheduledFuture<?> timeoutTask;
    private final Object stateLock = new Object();

    private final int timeoutSeconds ;
    private final int faulty; 

    private int quorum() {
        return 2 * faulty + 1;
    } 

    public MessageHandler(PortLink network,
                        Blockchain blockchain,
                        BlockingQueue<Message> inbound,
                        int timeoutSeconds){
        this.inbound = inbound;
        this.network = network;
        this.blockchain = blockchain;
        this.timeoutSeconds = timeoutSeconds;

        this.myId = blockchain.getId();
        this.faulty = blockchain.getFaulty();
        int n = network.getPortCount();

        if (n < 3 * faulty + 1) {
            throw new IllegalArgumentException();
        }
        startRouter();
    }
    
    private int incrementRound() { return round.incrementAndGet(); }
    private int getCurrentRound() { return round.get(); }

    public void resetState() {
        synchronized(stateLock) {
            hasVoted.set(false);
            proposedBlocks.clear();
            voteMap.clear();
        }
    }

    private void startTimeout() {
        cancelTimeout();
        timeoutTask = scheduler.schedule(() -> {
            resetState();
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    private void cancelTimeout() {
        if (timeoutTask != null && !timeoutTask.isDone()) {
            timeoutTask.cancel(true);  
        }
    }

    private int addVote(String blockId, String voterId, Block newBlock) {
        proposedBlocks.putIfAbsent(blockId, newBlock);
        voteMap.computeIfAbsent(blockId, _ -> ConcurrentHashMap.newKeySet()).add(voterId);
        return  voteMap.get(blockId).size();
    }

    public void startRouter() {
        new Thread(() -> {
            while (true) {
                try {
                    final Message msg = inbound.take();
                    messageRouter(msg); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println(e);
                }
            }
        }).start();
    }

    private void messageRouter(Message msg) {
        switch (msg.getType()) {
            case PROPOSE:
                proposeHandler(msg);
                break;
            case VOTE:
                voteHandler(msg);
                break;
            default:
        }
    }

    private void proposeHandler(Message msg){
        if (!hasVoted.compareAndSet(false, true)) return; 
        final Block newBlock = msg.getBlock();
        
        if(!blockchain.isValid(newBlock)) return;
        if(newBlock.getIndex()!=getCurrentRound()) return;

        final Message myVote = new Message(Message.Type.VOTE, myId, newBlock);
        network.broadcastMessage(myVote);

        voteHandler(myVote);

        startTimeout();
    }

    private void voteHandler(Message msg){
        if (msg.getBlock().getIndex() != getCurrentRound()) return;
        final int votes = addVote(msg.getBlockId(), msg.getSenderId(), msg.getBlock());
        if(votes >= quorum()) commitHandler(proposedBlocks.get(msg.getBlockId()));
    }

    private void commitHandler(Block newBlock){
        cancelTimeout();
        synchronized(stateLock) {
            blockchain.commitBlock(newBlock);
            resetState();  
            incrementRound();
        }
    }

} 

