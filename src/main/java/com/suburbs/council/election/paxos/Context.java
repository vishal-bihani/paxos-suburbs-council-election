package com.suburbs.council.election.paxos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suburbs.council.election.Member;
import com.suburbs.council.election.Node;
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

        state = null;
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
        calculateMajorityNumber();
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
        majorityNumber = (totalNodes / 2) + 1;
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

    public String getNewProposalNumber() {
        return PaxosUtils.generatePrepareNumber(lastPrepareMessageId, node.getId());
    }

    public void putIncomingMessageToQueue(String incomingMessage) throws InterruptedException {
        receivedMessages.put(incomingMessage);
    }

    public void savePrepareMessage(Long prepareMessageNumber, Prepare prepare) {
        receivedPrepareMessages.put(prepareMessageNumber, prepare);

        lastPrepareMessageId = prepareMessageNumber;
        lastPrepareMessageIdWithNodeId = prepare.getNewPrepareMessageId();
    }

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

    public int getCurrentVotes(Long prepareMessageNumber) {
        Integer currentVotes = votesPerPrepare.get(prepareMessageNumber);

        currentVotes = currentVotes == null ? 0 : currentVotes;
        return currentVotes;
    }

    public boolean isMajorityVotesReceived(Long prepareMessageNumber) {
        int currentVotes = getCurrentVotes(prepareMessageNumber);
        return currentVotes >= majorityNumber;
    }

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

    public int getCurrentNoOfPromises(Long prepareMessageNumber) {
        Integer currentNumberOfPromises = promisesPerPrepare.get(prepareMessageNumber);

        currentNumberOfPromises = currentNumberOfPromises == null ? 0 : currentNumberOfPromises;
        return currentNumberOfPromises;
    }

    public boolean isMajorityPromisesReceived(Long prepareMessageNumber) {
        int currentNumberOfPromises = getCurrentNoOfPromises(prepareMessageNumber);
        return currentNumberOfPromises >= majorityNumber;
    }

    public BlockingQueue<String> getReceivedMessages() {
        return receivedMessages;
    }

    public long getLastPrepareMessageId() {
        return lastPrepareMessageId;
    }

    public String getLastPrepareMessageIdWithNodeId() {
        return lastPrepareMessageIdWithNodeId;
    }

    public void setLastPrepareMessageId(long prepareMessageId) {
        this.lastPrepareMessageId = prepareMessageId;
    }

    public void setLastPrepareMessageIdWithNodeId(String prepareMessageIdWithNodeId) {
        this.lastPrepareMessageIdWithNodeId = prepareMessageIdWithNodeId;
    }

    public Prepare getLastPrepareMessage() {
        return receivedPrepareMessages.get(lastPrepareMessageId);
    }

    public String getCurrentAcceptedPrepareMessageId() {
        return currentAcceptedPrepareMessageId;
    }

    public void setCurrentAcceptedPrepareMessageId(String currentAcceptedPrepareMessageId) {
        this.currentAcceptedPrepareMessageId = currentAcceptedPrepareMessageId;
    }

    public void updateState(String state) {
        this.state = state;
    }

    public String getCurrentState() {
        return state;
    }

    public int getInitProposeDelay() {
        return node.getInitProposeDelay();
    }
}
