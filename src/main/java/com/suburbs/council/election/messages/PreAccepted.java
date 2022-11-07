package com.suburbs.council.election.messages;

import com.suburbs.council.election.paxos.Context;

/**
 * PreAccepted will be broadcast by the Acceptors to all the nodes.
 * If the acceptors receive (a + 3f+ 1) / 2 votes, majority is achieved and
 * then {@link Accepted} will be sent to the Proposer.
 */
public class PreAccepted implements Message {

    private Message.Type messageType;
    private int proposerNodeId;
    private int responderNodeId;
    private String proposedPrepareMessageId;

    /**
     * Constructor.
     *
     * @param context Context object holds the resources which are shared among all the threads
     * @param proposedPrepareMessageId Identifier of the PREPARE message
     * @param proposerNodeId Node id of the proposer
     */
    public PreAccepted(Context context, String proposedPrepareMessageId, int proposerNodeId) {
        this.messageType = Type.PREACCEPTED;

        this.responderNodeId = context.getNodeId();
        this.proposedPrepareMessageId = proposedPrepareMessageId;
        this.proposerNodeId = proposerNodeId;
    }

    // No-args used by Jackson
    public PreAccepted() {
    }

    public int getProposerNodeId() {
        return proposerNodeId;
    }

    public void setProposerNodeId(int proposerNodeId) {
        this.proposerNodeId = proposerNodeId;
    }

    public int getResponderNodeId() {
        return responderNodeId;
    }

    public void setResponderNodeId(int responderNodeId) {
        this.responderNodeId = responderNodeId;
    }

    public String getProposedPrepareMessageId() {
        return proposedPrepareMessageId;
    }

    public void setProposedPrepareMessageId(String proposedPrepareMessageId) {
        this.proposedPrepareMessageId = proposedPrepareMessageId;
    }

    /**
     * Get the type of the message.
     *
     * @return Type of the message
     */
    @Override
    public Type getMessageType() {
        return messageType;
    }
}
