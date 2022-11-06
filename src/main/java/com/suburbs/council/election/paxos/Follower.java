package com.suburbs.council.election.paxos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.suburbs.council.election.messages.*;
import com.suburbs.council.election.utils.Utils;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Follower extends PaxosMember {
    private static final Logger log = LoggerFactory.getLogger(Follower.class);

    private final Context context;

    public Follower(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {

            BlockingQueue<String> receivedMessages = context.getReceivedMessages();
            while(!context.getReceivedMessages().isEmpty()) {
                try {

                    String message = receivedMessages.poll();
                    Message.Type messageType;
                    try {
                        messageType = Utils.getMessageType(message);

                    } catch (JsonProcessingException e) {
                        log.error("[{}]: Error processing message with exception: {}",
                                context.getNodeName(), e.getMessage());

                        continue;
                    }

                    switch (messageType) {
                        case PREPARE -> handlePrepareMessage(message);
                        case ACCEPT -> handleAcceptMessage(message);
                        case ACCEPTED -> handleAcceptedMessage(message);
                    }
                } catch (Exception e) {
                    log.error("[{}]: Error handling message with exception: {}",
                            context.getNodeName(), e.getMessage());
                }
            }
        }
    }

    public void handlePrepareMessage(String message) throws JsonProcessingException {
        Prepare prepare = Utils.deserialize(message, Prepare.class);
        log.info("[{}]: Received prepare message from member: {}",
                context.getNodeName(), prepare.getProposerNodeName());

        long prepareMessageId = Utils.parsePrepareNumer(
                prepare.getNewPrepareMessageId()
        );


        // Check if higher prepare message ids are already processed
        if (!isHighestPrepareMessageId(prepareMessageId)) {
            // Since it is lower than the already existing message id
            // send a REJECT message.

            Reject reject = new Reject(context, context.getLastPrepareMessageId(), prepare.getNewPrepareMessageId());
            log.info("[{}]: Prepare message id {} is lower than existing message id {}, thus will be rejected",
                    context.getNodeName(),
                    prepare.getNewPrepareMessageId(),
                    context.getLastPrepareMessageId());

            dispatchRejectMessageToProposer(prepare.getProposerNodeId(), reject);
            return;
        }

        // Highest prepare message id. Will save and dispatch promise msg and increment votes.
        context.savePrepareMessage(prepareMessageId, prepare);
        Promise promise = new Promise(context, prepare.getNewPrepareMessageId(), prepare.getProposerNodeId());

        // Check if there is any saved prepare message id for which accepted was broadcast but
        // did not get majority votes yet
        if (context.getLastPrepareMessageIdWithNodeId() != null && context.getCurrentAcceptedPrepareMessageId() != null) {
            if (context.getLastPrepareMessageIdWithNodeId().equals(context.getCurrentAcceptedPrepareMessageId())) {

                // Add the last prepared message to the promise message
                promise.setLastPrepareMessage(context.getLastPrepareMessage());
            }
        }

        // Dispatch to the proposer
        dispatchPromiseMessageToProposer(promise.getProposerNodeId(), promise);
    }

    private void dispatchRejectMessageToProposer(int proposerNodeId, Reject reject) {
        context.getMembers()
                .forEach(member -> {
                    if (member.getId() != proposerNodeId) {
                        return;
                    }
                    try {
                        log.info("[{}]: Dispatching REJECT message to {}", context.getNodeName(),
                                member.getName());
                        Utils.dispatch(member, reject);

                    } catch (IOException e) {
                        log.error("[{}]: Error dispatching REJECT message for prepare message id: {}",
                                context.getNodeName(), reject.getProposedPrepareMessageId());
                    }
                });
    }

    private void dispatchPromiseMessageToProposer(int proposerNodeId, Promise promise) {
        context.getMembers()
                .forEach(member -> {
                    if (member.getId() != proposerNodeId) {
                        return;
                    }
                    try {
                        log.info("[{}]: Dispatching PROMISE message to {}", context.getNodeName(),
                                member.getName());
                        Utils.dispatch(member, promise);

                    } catch (IOException e) {
                        log.error("[{}]: Error dispatching PROMISE message for prepare message id: {}",
                                context.getNodeName(), promise.getPrepareMessageId());
                    }
                });
    }

    public void handleAcceptMessage(String message) throws JsonProcessingException {
        Accept accept = Utils.deserialize(message, Accept.class);
        log.info("[{}]: Received accept message from member: {}",
                context.getNodeName(), accept.getProposerNodeName());

        // Check if the accept message is for last proposed prepare message
        if (!accept.getPrepareMessageId().equals(context.getLastPrepareMessageIdWithNodeId())) {
            // It's another prepare message.

            long receivedPrepareMessageId = Utils.parsePrepareNumer(
                    accept.getPrepareMessageId()
            );
            if (!isHighestPrepareMessageId(receivedPrepareMessageId)) {
                // Prepared message id is lower than existing prepare message id, thus will be rejected.
                Reject reject = new Reject(context, context.getLastPrepareMessageId(), accept.getPrepareMessageId());
                dispatchRejectMessageToProposer(accept.getProposerNodeId(), reject);
            }

            // Ignore ACCEPT messages which are not associated with any previous PREPARE & PROMISE msg
            return;
        }

        // Including the PREPARE message to update state on those members which were not able to connect with
        // proposer
        Accepted accepted = new Accepted(context, accept.getPrepareMessageId(), context.getLastPrepareMessage());
        broadcastAcceptedMessage(accepted);

        long prepareMessageId = Utils.parsePrepareNumer(
                accept.getPrepareMessageId());

        context.incrementVotesForPrepare(prepareMessageId);
        if (context.isMajorityVotesReceived(prepareMessageId)) {
            context.updateState(
                    accepted.getProposedPrepareMessage()
                            .getProposal()
                            .getProposedMessage()
            );
        }
    }

    public void broadcastAcceptedMessage(Accepted accepted) {
        context.getMembers()
                .forEach(member -> {
                    try {
                        log.info("[{}]: Dispatching ACCEPTED message to {}", context.getNodeName(),
                                member.getName());
                        Utils.dispatch(member, accepted);

                    } catch (IOException e) {
                        log.error("[{}]: Error dispatching ACCEPTED message for prepare message id: {}",
                                context.getNodeName(), accepted.getPrepareMessageId());
                    }
                });
    }

    private void handleAcceptedMessage(String message) throws JsonProcessingException {
        Accepted accepted = Utils.deserialize(message, Accepted.class);
        log.info("[{}]: Received ACCEPTED message from member: {}",
                context.getNodeName(), accepted.getResponderNodeName());

        long prepareMessageId = Utils.parsePrepareNumer(
                accepted.getPrepareMessageId()
        );

        context.incrementVotesForPrepare(prepareMessageId);
        if (context.isMajorityVotesReceived(prepareMessageId)) {
            context.updateState(
                    accepted.getProposedPrepareMessage()
                            .getProposal()
                            .getProposedMessage()
            );
        }
    }

    private boolean isHighestPrepareMessageId(long prepareMessageId) {
        return prepareMessageId > context.getLastPrepareMessageId();
    }
}
