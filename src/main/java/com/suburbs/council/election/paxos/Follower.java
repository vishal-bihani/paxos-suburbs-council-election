package com.suburbs.council.election.paxos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.suburbs.council.election.enums.ResponseTiming;
import com.suburbs.council.election.messages.*;
import com.suburbs.council.election.utils.PaxosUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Follower is a Member who will not initiate or participate in election. Follower
 * can only vote.
 */
public class Follower extends PaxosMember {
    private static final Logger log = LoggerFactory.getLogger(Follower.class);

    private final Context context;
    private final int majorityNumber;
    private final ResponseTiming responseTiming;
    private final BlockingQueue<String> receivedMessages;
    private final Map<Long, Prepare> savedPrepareMessages;
    private final Map<Long, Integer> numberOfPrePromiseMessages;
    private final Map<Long, Integer> numberOfPreAcceptMessages;

    private final Map<Long, Boolean> dispatchedPromiseMessages;
    private final Map<Long, Boolean> dispatchedAcceptedMessages;

    /**
     * Constructor.
     *
     * @param context Context object holds the resources which are shared among all the threads
     */
    public Follower(Context context) {
        this.context = context;
        this.majorityNumber = context.getMajorityNumber();
        this.receivedMessages = context.getReceivedMessages();
        this.responseTiming = context.getResponseTiming();

        this.savedPrepareMessages = new HashMap<>();
        this.numberOfPrePromiseMessages = new HashMap<>();
        this.numberOfPreAcceptMessages = new HashMap<>();

        this.dispatchedPromiseMessages = new HashMap<>();
        this.dispatchedAcceptedMessages = new HashMap<>();
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            handleRequests();
        }
    }

    /**
     * Polls the queued messages and dispatches them to their handlers
     */
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
                    case PREPARE -> handlePrepareMessage(message);
                    case ACCEPT -> handleAcceptMessage(message);
                    case ACCEPTED -> handleAcceptedMessage(message);
                    case PREPROMISE -> handlePrePromiseMessage(message);
                    case PREACCEPTED -> handlePreAcceptedMessage(message);
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
     * Handles {@link Prepare} messages received from Proposer.
     *
     * @param message Prepare message
     * @throws JsonProcessingException Thrown if it encounters error while deserialization
     */
    public void handlePrepareMessage(String message) throws JsonProcessingException {
        Prepare prepare = PaxosUtils.deserialize(message, Prepare.class);
        log.info("[{}]: Received prepare message from member: {} with id: {}",
                context.getNodeName(), prepare.getProposerNodeName(), prepare.getNewPrepareMessageId());

        long prepareMessageId = PaxosUtils.parsePrepareNumer(
                prepare.getNewPrepareMessageId()
        );

        // Check if higher prepare message ids are already processed
        if (!isHighestPrepareMessageId(prepareMessageId)) {
            // Since it is lower than the already existing message id
            // send a REJECT message.

            Reject reject = new Reject(context, context.getLastPrepareMessageId(), prepare.getNewPrepareMessageId());
            log.info("[{}]: Prepare message id {} is not higher than existing message id {}, thus will be rejected",
                    context.getNodeName(),
                    prepare.getNewPrepareMessageId(),
                    context.getLastPrepareMessageId());

            dispatchRejectMessageToProposer(prepare.getProposerNodeId(), reject);
            return;
        }

        // Byzantine algo comes to effect. Broadcast PrePromise messages.

        PrePromise prePromise = new PrePromise(context, prepare.getNewPrepareMessageId(),
                prepare.getProposerNodeId(), prepare.getProposal());

        incrementValue(this.numberOfPrePromiseMessages, prepareMessageId, Message.Type.PREPROMISE,
                prepare.getNewPrepareMessageId());

        broadcastPrePromiseMessage(prePromise);

        this.savedPrepareMessages
                .put(prepareMessageId, prepare);
    }

    /**
     * Dispatches {@link Reject} message to the Proposer.
     *
     * @param proposerNodeId Node id of the proposer
     * @param reject Reject message to dispatch
     */
    private void dispatchRejectMessageToProposer(int proposerNodeId, Reject reject) {
        context.getMembers()
                .forEach(member -> {
                    if (member.getId() != proposerNodeId) {
                        return;
                    }
                    try {
                        log.info("[{}]: Dispatching REJECT message to {} for id: {}",
                                context.getNodeName(),
                                member.getName(),
                                reject.getProposedPrepareMessageId());

                        PaxosUtils.dispatch(member, reject);

                    } catch (IOException e) {
                        log.error("[{}]: Error dispatching REJECT message for prepare message id: {}",
                                context.getNodeName(), reject.getProposedPrepareMessageId());
                    }
                });
    }

    /**
     * Dispatches {@link Promise} message to the Proposer.
     *
     * @param proposerNodeId Node id of the Proposer
     * @param promise Promise message to dispatch
     */
    private void dispatchPromiseMessageToProposer(int proposerNodeId, Promise promise) {
        context.getMembers()
                .forEach(member -> {
                    if (member.getId() != proposerNodeId) {
                        return;
                    }
                    try {
                        log.info("[{}]: Dispatching PROMISE message to {} for id: {}",
                                context.getNodeName(),
                                member.getName(),
                                promise.getPrepareMessageId());

                        PaxosUtils.dispatch(member, promise);

                    } catch (IOException e) {
                        log.error("[{}]: Error dispatching PROMISE message for prepare message id: {}",
                                context.getNodeName(), promise.getPrepareMessageId());
                    }
                });
    }

    /**
     * Handles {@link Accept} message from Proposer.
     *
     * @param message Accept message to dispatch
     * @throws JsonProcessingException Thrown if it encounters error while deserialization
     */
    public void handleAcceptMessage(String message) throws JsonProcessingException {
        Accept accept = PaxosUtils.deserialize(message, Accept.class);
        log.info("[{}]: Received accept message from member: {} for id: {}",
                context.getNodeName(), accept.getProposerNodeName(), accept.getPrepareMessageId());

        long receivedPrepareMessageId = PaxosUtils.parsePrepareNumer(
                accept.getPrepareMessageId()
        );

        // Check if the accept message is for last proposed prepare message
        if (!accept.getPrepareMessageId().equals(context.getLastPrepareMessageIdWithNodeId())) {
            // It's another prepare message.

            if (!isHighestPrepareMessageId(receivedPrepareMessageId)) {
                // Prepared message id is lower than existing prepare message id, thus will be rejected.
                Reject reject = new Reject(context, context.getLastPrepareMessageId(), accept.getPrepareMessageId());
                dispatchRejectMessageToProposer(accept.getProposerNodeId(), reject);
            }

            // Ignore ACCEPT messages which are not associated with any previous PREPARE & PROMISE msg
            return;
        }

        // Broadcast PreAccepted message.
        PreAccepted preAccepted = new PreAccepted(context, accept.getPrepareMessageId(), accept.getProposerNodeId());

        incrementValue(this.numberOfPrePromiseMessages, receivedPrepareMessageId, Message.Type.PREACCEPTED,
                accept.getPrepareMessageId());

        broadcastPreAcceptedMessage(preAccepted);
    }

    /**
     * Broadcasts {@link Accepted} messages to all the Members.
     *
     * @param accepted Accepted message to broadcast
     */
    public void broadcastAcceptedMessage(Accepted accepted) {
        context.getMembers()
                .forEach(member -> {
                    try {
                        log.info("[{}]: Dispatching ACCEPTED message to {} for id: {}",
                                context.getNodeName(),
                                member.getName(),
                                accepted.getPrepareMessageId());

                        PaxosUtils.dispatch(member, accepted);

                    } catch (IOException e) {
                        log.error("[{}]: Error dispatching ACCEPTED message for prepare message id: {}",
                                context.getNodeName(), accepted.getPrepareMessageId());
                    }
                });
    }

    /**
     * Broadcasts {@link PrePromise} messages to all the Members.
     *
     * @param prePromise PrePromise message to broadcast
     */
    public void broadcastPrePromiseMessage(PrePromise prePromise) {
        context.getMembers()
                .forEach(member -> {
                    try {
                        log.info("[{}]: Dispatching PREPROMISE message to {} for id: {}",
                                context.getNodeName(),
                                member.getName(),
                                prePromise.getProposedPrepareMessageId());

                        PaxosUtils.dispatch(member, prePromise);

                    } catch (IOException e) {
                        log.error("[{}]: Error dispatching PREPROMISE message for prepare message id: {}",
                                context.getNodeName(), prePromise.getProposedPrepareMessageId());
                    }
                });
    }

    /**
     * Broadcasts {@link PreAccepted} messages to all the Members.
     *
     * @param preAccepted PreAccepted message to broadcast
     */
    public void broadcastPreAcceptedMessage(PreAccepted preAccepted) {
        context.getMembers()
                .forEach(member -> {
                    try {
                        log.info("[{}]: Dispatching PREACCEPTED message to {} for id: {}",
                                context.getNodeName(),
                                member.getName(),
                                preAccepted.getProposedPrepareMessageId());

                        PaxosUtils.dispatch(member, preAccepted);

                    } catch (IOException e) {
                        log.error("[{}]: Error dispatching PREACCEPTED message for prepare message id: {}",
                                context.getNodeName(), preAccepted.getProposedPrepareMessageId());
                    }
                });
    }

    /**
     * Handles all the {@link Accepted} messages received from the members.
     *
     * @param message Accept message
     * @throws JsonProcessingException Throws if encounters any error while deserialization
     */
    public void handleAcceptedMessage(String message) throws JsonProcessingException {
        Accepted accepted = PaxosUtils.deserialize(message, Accepted.class);
        log.info("[{}]: Received ACCEPTED message from member: {} for id: {}",
                context.getNodeName(), accepted.getResponderNodeName(), accepted.getPrepareMessageId());

        // Parse the identifier
        long prepareMessageId = PaxosUtils.parsePrepareNumer(
                accepted.getPrepareMessageId()
        );

        // Increment total no. of votes for the identifier
        context.incrementVotesForPrepare(prepareMessageId);

        // Check if Majority is achieved. If yes, update the state
        if (context.isMajorityVotesReceived(prepareMessageId)) {
            context.updateState(
                    accepted.getProposedPrepareMessage()
                            .getProposal()
                            .getProposedMessage() + " for term: " + accepted.getPrepareMessageId()
            );
        }
    }

    /**
     * Handles all the {@link PrePromise} messages received from the members.
     *
     * @param message PrePromise message
     * @throws JsonProcessingException Throws if encounters any error while deserialization
     */
    public void handlePrePromiseMessage(String message) throws JsonProcessingException {
        PrePromise prePromise = PaxosUtils.deserialize(message, PrePromise.class);
        log.info("[{}]: Received PREPROMISE message from member: {} for id: {}",
                context.getNodeName(), prePromise.getResponderNodeId(), prePromise.getProposedPrepareMessageId());

        // Parse the identifier
        long prepareMessageId = PaxosUtils.parsePrepareNumer(
                prePromise.getProposedPrepareMessageId()
        );

        // Increment total no. of PrePromises for the identifier
        incrementValue(this.numberOfPrePromiseMessages, prepareMessageId,
                Message.Type.PREPROMISE, prePromise.getProposedPrepareMessageId());

        // Check if Majority is achieved. If yes, update the state
        if (!isByzantineMajorityReceived(this.numberOfPrePromiseMessages, prepareMessageId)) {
            return;
        }

        log.info("[{}]: Byzantine majority reached for {} with count: {}",
                context.getNodeName(), Message.Type.PREPROMISE, this.numberOfPrePromiseMessages.get(prepareMessageId));

        // Check if higher prepare message ids are already processed
        if (!isHighestPrepareMessageId(prepareMessageId)) {
            // Since it is lower than the already existing message id
            // send a REJECT message.

            Reject reject = new Reject(context, context.getLastPrepareMessageId(), prePromise.getProposedPrepareMessageId());
            log.info("[{}]: Message id {} is not higher than existing message id {}, thus will be rejected",
                    context.getNodeName(),
                    prePromise.getProposedPrepareMessageId(),
                    context.getLastPrepareMessageId());

            dispatchRejectMessageToProposer(prePromise.getProposerNodeId(), reject);
            this.savedPrepareMessages.remove(prepareMessageId);
            return;
        }

        Prepare prepare  = this.savedPrepareMessages
                .get(prepareMessageId);

        if (prepare == null) {

            log.info("[{}]: No PREPARE message from member: {} for message id: {}. Ignoring PREPROMISE",
                    context.getNodeName(), prePromise.getProposerNodeId(), prePromise.getProposedPrepareMessageId());

            return;
        }

        context.savePrepareMessage(prepareMessageId, prepare);
        Promise promise = new Promise(context, prePromise.getProposedPrepareMessageId(),
                prePromise.getProposerNodeId());

        // Check if there is any saved prepare message id for which accepted was broadcast but
        // did not get majority votes yet
        if (context.getLastPrepareMessageIdWithNodeId() != null && context.getCurrentAcceptedPrepareMessageId() != null) {
            if (context.getLastPrepareMessageIdWithNodeId().equals(context.getCurrentAcceptedPrepareMessageId())) {

                log.info("[{}]: Adding last proposal to PROMISE message to new proposer: {} for message id: {}",
                        context.getNodeName(), prepare.getProposerNodeName(), promise.getPrepareMessageId());

                // Add the last prepared message to the promise message
                promise.setLastPrepareMessage(context.getLastPrepareMessage());
            }
        }

        if (this.dispatchedPromiseMessages.containsKey(prepareMessageId)
                && Boolean.TRUE.equals(this.dispatchedPromiseMessages.get(prepareMessageId))) {


            log.info("[{}]: Already dispatched PROMISE message to proposer: {} for message id: {}",
                    context.getNodeName(), prepare.getProposerNodeName(), promise.getPrepareMessageId());
            return;
        }

        log.info("[{}]: Dispatching PROMISE message to proposer: {} for message id: {}",
                context.getNodeName(), prepare.getProposerNodeName(), promise.getPrepareMessageId());
        // Dispatch to the proposer
        dispatchPromiseMessageToProposer(promise.getProposerNodeId(), promise);
        this.dispatchedPromiseMessages.put(prepareMessageId, true);
    }

    /**
     * Handles all the {@link PreAccepted} messages received from the members.
     *
     * @param message PreAccepted message
     * @throws JsonProcessingException Throws if encounters any error while deserialization
     */
    public void handlePreAcceptedMessage(String message) throws JsonProcessingException {
        PreAccepted preAccepted = PaxosUtils.deserialize(message, PreAccepted.class);
        log.info("[{}]: Received PREACCEPTED message from member: {} for id: {}",
                context.getNodeName(), preAccepted.getProposerNodeId(), preAccepted.getProposedPrepareMessageId());

        // Parse the identifier
        long prepareMessageId = PaxosUtils.parsePrepareNumer(
                preAccepted.getProposedPrepareMessageId()
        );

        // Increment total no. of PrePromises for the identifier
        incrementValue(this.numberOfPreAcceptMessages, prepareMessageId,
                Message.Type.PREACCEPTED, preAccepted.getProposedPrepareMessageId());

        // Check if Majority is achieved. If yes, update the state
        if (!isByzantineMajorityReceived(this.numberOfPrePromiseMessages, prepareMessageId)) {
            return;
        }

        if (this.dispatchedAcceptedMessages.containsKey(prepareMessageId)
                && Boolean.TRUE.equals(this.dispatchedAcceptedMessages.get(prepareMessageId))) {


            log.info("[{}]: Already broadcast ACCEPTED message for message id: {}",
                    context.getNodeName(), preAccepted.getProposedPrepareMessageId());
            return;
        }

        // Including the PREPARE message to update state on those members which were not able to connect with
        // proposer
        Accepted accepted = new Accepted(context, preAccepted.getProposedPrepareMessageId(),
                context.getLastPrepareMessage());

        context.incrementVotesForPrepare(prepareMessageId);
        broadcastAcceptedMessage(accepted);
        this.dispatchedAcceptedMessages.put(prepareMessageId, true);
    }

    /**
     * Checks if received number part of the identifier is higher than the existing
     * prepare message id.
     *
     * @param prepareMessageId Identifier of the received Prepare message
     * @return Is it higher than existing ids
     */
    private boolean isHighestPrepareMessageId(long prepareMessageId) {
        return prepareMessageId > context.getLastPrepareMessageId();
    }

    /**
     * Increments value in the given map.
     *
     * @param map Map where value will be incremented
     * @param key Key associated with the value
     */
    private void incrementValue(Map<Long, Integer> map, Long key, Message.Type messageType,
                                String prepareMessageNumber) {

        if (!map.containsKey(key)) {
            map.put(key, 0);
        }

        Integer currentValue = map.get(key);
        currentValue = currentValue == null ? 0 : currentValue;

        log.info("[{}]: Incrementing count for {} id: {} to {}",
                context.getNodeName(),
                messageType,
                prepareMessageNumber,
                currentValue + 1);

        map.replace(key, currentValue + 1);
    }

    private boolean isByzantineMajorityReceived(Map<Long, Integer> map, Long key) {
        return map.get(key) >= majorityNumber;
    }
}
