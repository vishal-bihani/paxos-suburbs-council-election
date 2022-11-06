package com.suburbs.council.election.paxos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.suburbs.council.election.messages.*;
import com.suburbs.council.election.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;

public class Candidate extends Follower {
    private static final Logger log = LoggerFactory.getLogger(PaxosMember.class);

    private final Context context;
    private boolean isLeader = false;
    private boolean broadcastedPrepareMessage = false;

    public Candidate(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {

            try {
                if (!isLeader && !broadcastedPrepareMessage) {
                    log.info("[{}]: Not a leader so will broadcast prepare messages", context.getNodeName());
                    Prepare prepare = new Prepare(context);
                    long prepareMessageId = Utils.parsePrepareNumer(
                            prepare.getNewPrepareMessageId()
                    );

                    broadcastPrepareMessage(prepare);
                    broadcastedPrepareMessage = true;
                    context.savePrepareMessage(prepareMessageId, prepare);
                    context.setLastPrepareMessageId(prepareMessageId);
                    context.setLastPrepareMessageIdWithNodeId(prepare.getNewPrepareMessageId());
                }

            } catch (Exception e) {
                log.error("[{}]: Failed to broadcast prepare message with exception: {}",
                        context.getNodeName(), e.getMessage());
            }

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

                        // Candidate related
                        case PROMISE -> handlePromiseMessages(message);

                        // Follower related
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
    }

    private void handlePromiseMessages(String message) throws JsonProcessingException {
        Promise promise = Utils.deserialize(message, Promise.class);
        log.info("[{}]: Received promise message from member: {}",
                context.getNodeName(), promise.getResponderNodeName());

        Long promiseMessageId = Utils.parsePrepareNumer(
                promise.getPrepareMessageId());

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

        if (!context.isMajorityPromisesReceived(promiseMessageId)) {
            return;
        }

        log.info("Majority promises received, time to dispatch accept messages");
        Accept accept = new Accept(context, promise.getPrepareMessageId());
        broadcastAcceptMessage(accept);
    }

    private void broadcastAcceptMessage(Accept accept) {
        context.getMembers()
                .forEach(member -> {
                    try {
                        log.info("[{}]: Dispatching ACCEPT message to {}", context.getNodeName(),
                                member.getName());
                        Utils.dispatch(member, accept);

                    } catch (IOException e) {
                        log.error("[{}]: Error broadcast ACCEPT message for prepare message id: {}",
                                context.getNodeName(), accept.getPrepareMessageId());
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
            isLeader = true;
        }
    }

    private void broadcastPrepareMessage(Prepare prepare) {
        context.getMembers()
                .forEach(member -> {
                    try {
                        log.info("[{}]: Dispatching PREPARE message to {}", context.getNodeName(),
                                member.getName());
                        Utils.dispatch(member, prepare);

                    } catch (IOException e) {
                        log.error("[{}]: Error broadcast PREPARE message for prepare message id: {} to {}",
                                context.getNodeName(), prepare.getNewPrepareMessageId(), member.getName());
                    }
                });
    }

    private void dispatchPrepareMessagesToMembers() throws JsonProcessingException {
        Prepare prepare = new Prepare(context);
        Long prepareMessageId = Utils.parsePrepareNumer(
                prepare.getNewPrepareMessageId()
        );
        String serializedPrepare = Utils.serialize(prepare);
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
}
