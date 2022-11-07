package com.suburbs.council.election.paxos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suburbs.council.election.Member;
import com.suburbs.council.election.Node;
import com.suburbs.council.election.enums.ResponseTiming;
import com.suburbs.council.election.messages.HeartBeat;
import com.suburbs.council.election.messages.Prepare;
import com.suburbs.council.election.paxos.service.MonitoringService;
import com.suburbs.council.election.utils.PaxosUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context class contains synchronized resources to be accessed by multithreaded services.
 */
public class Context {
    private static final Logger log = LoggerFactory.getLogger(Context.class);

    private final Node node;
    private final ObjectMapper mapper;
    private final List<Member> members;

    private Server server;
    private int totalNodes;
    private int majorityNumber;
    private Long lastPrepareMessageId = 0L;
    private int totalByzantineFaultsSupported;
    private String currentAcceptedPrepareMessageId;
    private MonitoringService monitoringService;
    private String lastPrepareMessageIdWithNodeId;

    private final Map<Long, Integer> votesPerPrepare;
    private final Map<Long, Integer> promisesPerPrepare;
    private final BlockingQueue<String> receivedMessages;
    private final BlockingQueue<HeartBeat> heartBeatMessages;
    private final Map<Long, Prepare> receivedPrepareMessages;

    private String state;

    /**
     * Constructor.
     *
     * @param node Current node object
     * @param members List of member nodes
     */
    public Context(Node node, List<Member> members) {
        this.node = node;
        this.members = members;

        state = "Election yet to happen";
        mapper = new ObjectMapper();
        votesPerPrepare = new HashMap<>();
        promisesPerPrepare = new HashMap<>();
        receivedPrepareMessages = new HashMap<>();
        currentAcceptedPrepareMessageId = null;
        lastPrepareMessageIdWithNodeId = null;
        receivedMessages = new LinkedBlockingQueue<>();
        heartBeatMessages = new LinkedBlockingQueue<>();

        // Update the total number of nodes, as per the configuration file and
        // calculate the number of nodes needed for majority votes.
        updateTotalNumberOfNodes();
        calculateTotalNumberOfByzantineFaultsSupported();
        calculateMajorityNumber();
    }

    /**
     * As per Fast Byzantine paxos algorithm <pre><code>N >= 5M + 1</code></pre>
     * where N is the number of nodes and M is total number of byzantine faults.
     *
     * Assuming all the members as acceptors this will calculate total no. of
     * byzantine faults.
     */
    private void calculateTotalNumberOfByzantineFaultsSupported() {

        // The formula is (Total no. of members including proposer - 1) / 5 >= Byzantine faults.
        this.totalByzantineFaultsSupported = (int) ((totalNodes - 1) / 5);

        log.info("[{}]: Following assumption is being made that (proposers >= 3f + 1)", getNodeName());
        log.info("[{}]: Total no. of byzantine faults that can be supported is {}",
                getNodeName(), totalByzantineFaultsSupported);
    }

    /**
     * Returns the port number for the current node where
     * the server socket should bind to.
     *
     * @return port
     */
    public int getServerPort() {
        return node.getPort();
    }

    /**
     * Update total number of nodes.
     */
    private void updateTotalNumberOfNodes() {
        totalNodes = members.size() + 1;
    }

    /**
     * Calculate number of nodes/responses needed for majority.
     */
    private void calculateMajorityNumber() {
        // As per Fast Byzantine Paxos the formula for majority number is
        // votes >= (a + 3f + 1) / 2
        majorityNumber = (int) ((totalNodes + (3 * totalByzantineFaultsSupported) + 1) / 2);
    }

    /**
     * Returns the majority number.
     *
     * @return Majority number needed
     */
    public int getMajorityNumber() {
        return this.majorityNumber;
    }

    /**
     * Returns list of all members.
     *
     * @return List of members
     */
    public List<Member> getMembers() {
        return members;
    }

    /**
     * Returns the name of the current node.
     *
     * @return Name of the current node
     */
    public String getNodeName() {
        return node.getName();
    }

    /**
     * Returns the id of the current node.
     *
     * @return Id of the current node
     */
    public int getNodeId() {
        return node.getId();
    }

    /**
     * Sets the {@link #monitoringService}.
     */
    public void setMonitoringService(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * Returns the monitoring service.
     *
     * @return Monitoring service
     */
    public MonitoringService getMonitoringService() {
        return monitoringService;
    }

    /**
     * Sets the {@link #server}
     */
    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Puts heartbeat messages into blocking queue.
     *
     * @param heartBeat HeartBeat Message
     * @throws InterruptedException
     */
    public void putHeartBeatMessages(String heartBeat) throws InterruptedException, JsonProcessingException {
        HeartBeat hb = mapper.readValue(heartBeat, HeartBeat.class);
        this.heartBeatMessages.put(hb);
    }

    /**
     * Returns collection of heartbeat messages.
     *
     * @return List of heartbeat messages.
     */
    public List<HeartBeat> pollHeartBeatMessage() {
        if (!heartBeatMessages.isEmpty()) {
            return heartBeatMessages.stream()
                    .toList();
        }
        return Collections.emptyList();
    }

    /**
     * Generate new message id.
     *
     * @return new message id
     */
    public String getNewProposalNumber() {
        return PaxosUtils.generatePrepareNumber(lastPrepareMessageId, node.getId());
    }

    /**
     * Queues the incoming message. This will be polled by request handler.
     *
     * @param incomingMessage Incoming message
     * @throws InterruptedException Thrown if exception occurs
     */
    public void putIncomingMessageToQueue(String incomingMessage) throws InterruptedException {
        receivedMessages.put(incomingMessage);
    }

    /**
     * Saves given prepare message
     *
     * @param prepareMessageNumber message id
     * @param prepare Prepare message
     */
    public void savePrepareMessage(Long prepareMessageNumber, Prepare prepare) {
        receivedPrepareMessages.put(prepareMessageNumber, prepare);

        lastPrepareMessageId = prepareMessageNumber;
        lastPrepareMessageIdWithNodeId = prepare.getNewPrepareMessageId();
    }

    /**
     * Increment no. of votes received for the message id
     *
     * @param prepareMessageNumber message id
     */
    public void incrementVotesForPrepare(Long prepareMessageNumber) {

        if (!votesPerPrepare.containsKey(prepareMessageNumber)) {
            votesPerPrepare.put(prepareMessageNumber, 0);
        }


        Integer currentVotes = votesPerPrepare.get(prepareMessageNumber);

        currentVotes = currentVotes == null ? 0 : currentVotes;

        log.info("[{}]: Incrementing votes for prepare id: {} to {}",
                node.getName(),
                prepareMessageNumber,
                currentVotes + 1);
        votesPerPrepare.replace(prepareMessageNumber, currentVotes + 1);
    }

    /**
     * Get current votes for the message id.
     *
     * @param prepareMessageNumber message id
     * @return no. of votes
     */
    public int getCurrentVotes(Long prepareMessageNumber) {
        Integer currentVotes = votesPerPrepare.get(prepareMessageNumber);

        currentVotes = currentVotes == null ? 0 : currentVotes;
        return currentVotes;
    }

    /**
     * Checks if majority votes received.
     *
     * @param prepareMessageNumber message id
     * @return if majority votes received
     */
    public boolean isMajorityVotesReceived(Long prepareMessageNumber) {
        int currentVotes = getCurrentVotes(prepareMessageNumber);
        return currentVotes >= majorityNumber;
    }

    /**
     * Increment no. of promises received.
     *
     * @param prepareMessageNumber message id
     */
    public void incrementPromisesForPrepare(Long prepareMessageNumber) {

        if (!promisesPerPrepare.containsKey(prepareMessageNumber)) {
            promisesPerPrepare.put(prepareMessageNumber, 0);
        }

        Integer promises = promisesPerPrepare.get(prepareMessageNumber);

        promises = promises == null ? 0 : promises;

        log.info("[{}]: Incrementing promises for prepare id: {} to {}",
                node.getName(),
                prepareMessageNumber,
                promises + 1);
        promisesPerPrepare.replace(prepareMessageNumber, promises + 1);
    }

    /**
     * Get no. of promises received.
     *
     * @param prepareMessageNumber message id
     * @return no. of promises received
     */
    public int getCurrentNoOfPromises(Long prepareMessageNumber) {
        Integer currentNumberOfPromises = promisesPerPrepare.get(prepareMessageNumber);

        currentNumberOfPromises = currentNumberOfPromises == null ? 0 : currentNumberOfPromises;
        return currentNumberOfPromises;
    }

    /**
     * Checks if majority promises received.
     *
     * @param prepareMessageNumber Message id
     * @return is majority promises achieved
     */
    public boolean isMajorityPromisesReceived(Long prepareMessageNumber) {
        int currentNumberOfPromises = getCurrentNoOfPromises(prepareMessageNumber);
        return currentNumberOfPromises >= majorityNumber;
    }

    /**
     * Returns queue of received messages.
     *
     * @return Queue
     */
    public BlockingQueue<String> getReceivedMessages() {
        return receivedMessages;
    }

    /**
     * Returns message id of the last prepare message.
     *
     * @return message id
     */
    public long getLastPrepareMessageId() {
        return lastPrepareMessageId;
    }

    /**
     * Returns formatted message id of the last saved prepare message.
     *
     * @return formatted message id
     */
    public String getLastPrepareMessageIdWithNodeId() {
        return lastPrepareMessageIdWithNodeId;
    }

    /**
     * Set message id of the last saved prepare message.
     *
     * @param prepareMessageId message id
     */
    public void setLastPrepareMessageId(long prepareMessageId) {
        this.lastPrepareMessageId = prepareMessageId;
    }

    /**
     * Get formatted message id of the last saved prepare message.
     *
     * @param prepareMessageIdWithNodeId Formatted message id
     */
    public void setLastPrepareMessageIdWithNodeId(String prepareMessageIdWithNodeId) {
        this.lastPrepareMessageIdWithNodeId = prepareMessageIdWithNodeId;
    }

    /**
     * Get last saved prepare message
     *
     * @return Prepare message
     */
    public Prepare getLastPrepareMessage() {
        return receivedPrepareMessages.get(lastPrepareMessageId);
    }

    /**
     * Get message id of the current accepted message.
     *
     * @return message id
     */
    public String getCurrentAcceptedPrepareMessageId() {
        return currentAcceptedPrepareMessageId;
    }

    /**
     * Set current accepted prepare message id.
     *
     * @param currentAcceptedPrepareMessageId message id
     */
    public void setCurrentAcceptedPrepareMessageId(String currentAcceptedPrepareMessageId) {
        this.currentAcceptedPrepareMessageId = currentAcceptedPrepareMessageId;
    }

    /**
     * Update state.
     *
     * @param state updated state
     */
    public void updateState(String state) {
        this.state = state;
    }

    /**
     * Get current state.
     *
     * @return state
     */
    public String getCurrentState() {
        return state;
    }

    /**
     * Get configured initial delay for initiating election
     *
     * @return delay
     */
    public int getInitProposeDelay() {
        return node.getInitProposeDelay();
    }

    /**
     * Get {@link ResponseTiming} category
     *
     * @return ResponseTiming
     */
    public ResponseTiming getResponseTiming() {
        return node.getResponseTiming();
    }
}
