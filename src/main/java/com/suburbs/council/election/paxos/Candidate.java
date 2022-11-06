package com.suburbs.council.election.paxos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.suburbs.council.election.messages.*;
import com.suburbs.council.election.utils.PaxosUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;

/**
 * Candidates are those Members who can initiate and participate an election. They can also vote.
 * Candidates are also Followers
 */
public class Candidate extends Follower {
    private static final Logger log = LoggerFactory.getLogger(Candidate.class);
    private static final int MAX_INTERVAL = 20;
    private static final int MIN_INTERVAL = 10;

    private final Context context;
    private long resetStartTime;
    private int intervalBetweenInitiatingElection;
    private final BlockingQueue<String> receivedMessages;

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
    private void handleRequests() {
        while(!receivedMessages.isEmpty()) {

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
                }

            } catch (Exception e) {
                log.error("[{}]: Error handling message with exception: {}",
                        context.getNodeName(), e.getMessage());
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
        log.info("[{}]: Received promise message from member: {}",
                context.getNodeName(), promise.getResponderNodeName());

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

        log.info("Majority promises received, time to dispatch accept messages");
        Accept accept = new Accept(context, promise.getPrepareMessageId());
        broadcastAcceptMessage(accept);
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
     * Handles all the {@link Accepted} messages received from the members.
     *
     * @param message Accept message
     * @throws JsonProcessingException Throws if encounters any error while deserialization
     */
//    private void handleAcceptedMessage(String message) throws JsonProcessingException {
//        Accepted accepted = PaxosUtils.deserialize(message, Accepted.class);
//        log.info("[{}]: Received ACCEPTED message from member: {}",
//                context.getNodeName(), accepted.getResponderNodeName());
//
//        // Parse the identifier
//        long prepareMessageId = PaxosUtils.parsePrepareNumer(
//                accepted.getPrepareMessageId()
//        );
//
//        // Increment total no. of votes for the identifier
//        context.incrementVotesForPrepare(prepareMessageId);
//
//        // Check if Majority is achieved. If yes, update the state
//        if (context.isMajorityVotesReceived(prepareMessageId)) {
//            context.updateState(
//                    accepted.getProposedPrepareMessage()
//                            .getProposal()
//                            .getProposedMessage()
//            );
//        }
//    }

    /**
     * Broadcast the {@link Prepare} message to all the members.
     *
     * @param prepare Prepare message to broadcast
     */
    private void broadcastPrepareMessage(Prepare prepare) {
        context.getMembers()
                .forEach(member -> {
                    try {
                        log.info("[{}]: Dispatching PREPARE message to {}", context.getNodeName(),
                                member.getName());
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

    private void dispatchPrepareMessagesToMembers() throws JsonProcessingException {
        Prepare prepare = new Prepare(context);
        Long prepareMessageId = PaxosUtils.parsePrepareNumer(
                prepare.getNewPrepareMessageId()
        );
        String serializedPrepare = PaxosUtils.serialize(prepare);
        context.savePrepareMessage(prepareMessageId, prepare);

        log.info("[{}]: Dispatching prepare messages to all members", context.getNodeName());
        context.getMembers()
                .forEach(member -> {
                    PrintWriter out = null;
                    try {
                        out = new PrintWriter(member.socket().getOutputStream(), true);

                    } catch (IOException e) {
                        log.error("[{}]: Error while dispatching prepare messages -> {}",
                                context.getNodeName(), e.getMessage());
                    }
                    out.println(serializedPrepare);
                    out.close();
                });
    }

    private int generateRandomIntervalBetweenElections() {
        return (int) ((Math.random() * (MAX_INTERVAL - MIN_INTERVAL)) + MIN_INTERVAL);
    }
}
