package com.suburbs.council.election.paxos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.suburbs.council.election.enums.ResponseTiming;
import com.suburbs.council.election.messages.*;
import com.suburbs.council.election.utils.PaxosUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;

/**
 * Candidates are those Members who can initiate and participate an election. They can also vote.
 * Candidates are also Followers
 */
public class Candidate extends Follower {
    private static final Logger log = LoggerFactory.getLogger(Candidate.class);
    private static final int MAX_INTERVAL = 120;
    private static final int MIN_INTERVAL = 60;
    public static final int MAX_PREPARE_ID_ADD = 10;
    public static final int MIN_PREPARE_ID_ADD = 5;


    private final Context context;
    private long resetStartTime;
    private final ResponseTiming responseTiming;
    private int intervalBetweenInitiatingElection;
    private final BlockingQueue<String> receivedMessages;

    private boolean dispatchedAcceptMessages;

    /**
     * Constructor.
     *
     * @param context Context object holds the resources which are shared among all the threads
     */
    public Candidate(Context context) {
        super(context);
        this.context = context;
        this.intervalBetweenInitiatingElection = context.getInitProposeDelay();
        this.resetStartTime = Instant.now()
                .getEpochSecond();

        this.receivedMessages = context.getReceivedMessages();
        this.responseTiming = context.getResponseTiming();

        this.dispatchedAcceptMessages = false;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {

            long currentTimestamp = Instant.now()
                    .getEpochSecond();

            // The candidate should wait till init propose delay to be able to
            // initiate a new election
            if ((currentTimestamp - resetStartTime) > intervalBetweenInitiatingElection) {
                initiateElection();
            }
            handleRequests();
        }
    }

    /**
     * Polls the queued messages and dispatches them to their handlers
     */
    @Override
    public void handleRequests() {
        while(!receivedMessages.isEmpty()) {

            // If response timing is set to be either of MEDIUM, LATE, NEVER, the thread
            // will sleep for that much of time
            delayResponseIfConfigured();

            try {

                String message = receivedMessages.poll();
                Message.Type messageType;
                try {
                    // Get the message type
                    messageType = PaxosUtils.getMessageType(message);

                } catch (JsonProcessingException e) {
                    log.error("[{}]: Error processing message with exception: {}",
                            context.getNodeName(), e.getMessage());
                    continue;
                }

                // Dispatch the messages to their handlers
                switch (messageType) {
                    case PROMISE -> handlePromiseMessages(message);
                    case ACCEPTED -> handleAcceptedMessage(message);
                    case PREPARE -> handlePrepareMessage(message);
                    case ACCEPT -> handleAcceptMessage(message);
                    case REJECT -> handleRejectMessage(message);
                }

            } catch (Exception e) {
                log.error("[{}]: Error handling message with exception: {}",
                        context.getNodeName(), e.getMessage());
            }
        }
    }

    /**
     * Delays the execution as per the configuration of {@link ResponseTiming}
     * in {@link com.suburbs.council.election.Node}.
     */
    @Override
    public void delayResponseIfConfigured() {
        long responseDelay = responseTiming.getResponseDelay();

        if (responseDelay > 0) {
            try {
                log.info("[{}]: Response timing configured as {}. Delaying response for {} ms",
                        context.getNodeName(),
                        responseTiming,
                        responseDelay);

                Thread.sleep(responseDelay);

            } catch (InterruptedException e) {
                log.error("[{}]: Delay interrupted", context.getNodeName());
            }
        }
    }

    /**
     * Handles {@link Promise} messages that are sent by the members.
     *
     * @param message Promise message
     * @throws JsonProcessingException Throws if encounters any error while deserialization
     */
    private void handlePromiseMessages(String message) throws JsonProcessingException {
        Promise promise = PaxosUtils.deserialize(message, Promise.class);
        log.info("[{}]: Received promise message from member: {} for id: {}",
                context.getNodeName(), promise.getResponderNodeName(), promise.getPrepareMessageId());

        // Parse the identifier
        long promiseMessageId = PaxosUtils.parsePrepareNumer(
                promise.getPrepareMessageId());

        // Increment total no. of promises
        context.incrementPromisesForPrepare(promiseMessageId);

        // If there was a PREPARE message accepted by other nodes but not received by this node
        // then this node has to update its proposal to last prepare message and send that in
        // the ACCEPT message
        if (promise.getLastPrepareMessage() != null) {
            context.getLastPrepareMessage()
                    .getProposal()
                    .setProposedMessage(
                            promise.getLastPrepareMessage()
                                    .getProposal()
                                    .getProposedMessage()
                    );
        }

        // Check if the majority of the promises are received. If yes, then proceed with broadcasting
        // Accept messages
        if (!context.isMajorityPromisesReceived(promiseMessageId)) {
            return;
        }

        // If Accept messages are already dispatched. Then DO NOT
        // re-dispatch it.
        if (dispatchedAcceptMessages) {
            return;
        }

        log.info("Majority promises received for id: {}, time to dispatch accept messages",
                promise.getPrepareMessageId());

        Accept accept = new Accept(context, promise.getPrepareMessageId());
        broadcastAcceptMessage(accept);
        dispatchedAcceptMessages = true;
    }

    /**
     * Broadcasts the {@link Accept} messages to all the members.
     *
     * @param accept Accept message
     */
    private void broadcastAcceptMessage(Accept accept) {
        context.getMembers()
                .forEach(member -> {
                    try {
                        log.info("[{}]: Dispatching ACCEPT message to {}", context.getNodeName(),
                                member.getName());
                        PaxosUtils.dispatch(member, accept);

                    } catch (IOException e) {
                        log.error("[{}]: Error broadcast ACCEPT message for prepare message id: {}",
                                context.getNodeName(), accept.getPrepareMessageId());
                    }
                });
    }

    /**
     * Broadcast the {@link Prepare} message to all the members.
     *
     * @param prepare Prepare message to broadcast
     */
    private void broadcastPrepareMessage(Prepare prepare) {
        context.getMembers()
                .forEach(member -> {
                    try {
                        log.info("[{}]: Dispatching PREPARE message to {} with id: {}",
                                context.getNodeName(),
                                member.getName(),
                                prepare.getNewPrepareMessageId());

                        PaxosUtils.dispatch(member, prepare);

                    } catch (IOException e) {
                        log.error("[{}]: Error broadcast PREPARE message for prepare message id: {} to {}",
                                context.getNodeName(), prepare.getNewPrepareMessageId(), member.getName());
                    }
                });
    }

    /**
     * Initiates new election by broadcasting {@link Prepare} message.
     */
    private void initiateElection() {

        // Needed to handle multiple dispatches with each increasing vote over majority mark.
        this.dispatchedAcceptMessages = false;

        try {
            // Initiate election
            log.info("[{}]: Satisfied propose delay condition. Now initiating new election",
                    context.getNodeName());

            Prepare prepare = new Prepare(context);
            long prepareMessageId = PaxosUtils.parsePrepareNumer(
                    prepare.getNewPrepareMessageId()
            );

            broadcastPrepareMessage(prepare);
            context.savePrepareMessage(prepareMessageId, prepare);
            context.setLastPrepareMessageId(prepareMessageId);
            context.setLastPrepareMessageIdWithNodeId(prepare.getNewPrepareMessageId());

        } catch (Exception e) {
            log.error("[{}]: Failed to broadcast prepare message with exception: {}",
                    context.getNodeName(), e.getMessage());

        } finally {

            // Once Prepare messages are broadcast, the interval and the start time has to reset.
            this.intervalBetweenInitiatingElection = generateRandomIntervalBetweenElections();
            this.resetStartTime = Instant.now()
                    .getEpochSecond();
        }
    }

    /**
     * Handles {@link Reject} message.
     *
     * @param message Reject message
     */
    private void handleRejectMessage(String message) throws JsonProcessingException {
        Reject reject = PaxosUtils.deserialize(message, Reject.class);
        log.info("[{}]: Received REJECT message from member: {} for id: {}",
                context.getNodeName(), reject.getResponderNodeId(), reject.getProposedPrepareMessageId());

        // Check if this regarding current node's stored PREPARE message id.
        if (context.getLastPrepareMessageIdWithNodeId().equals(reject.getProposedPrepareMessageId())) {
            // This is regarding the correct proposed id.
            // Update the state.

            context.setLastPrepareMessageId(
                    reject.getCurrentPrepareMessageId()
            );
            context.setLastPrepareMessageIdWithNodeId(
                    PaxosUtils.generatePrepareNumber(
                            reject.getCurrentPrepareMessageId(), reject.getResponderNodeId()
                    )
            );
        }
    }

    /**
     * Generates random interval between elections.
     *
     * @return Random interval
     */
    private int generateRandomIntervalBetweenElections() {
        return PaxosUtils.generateRandomNumber(MAX_INTERVAL, MIN_INTERVAL);
    }
}
