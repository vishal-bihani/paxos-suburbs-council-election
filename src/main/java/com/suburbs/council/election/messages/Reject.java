package com.suburbs.council.election.messages;

import com.suburbs.council.election.paxos.Context;

/**
 * Reject message is send by the Member to Proposer either during the
 * Prepare phase (if the Prepare message identifier is invalid) or during
 * the Accept phase rejecting the Proposed change.
 */
public class Reject implements Message {

    private int responderNodeId;
    private Type messageType;
    private long currentPrepareMessageId;
    private String proposedPrepareMessageId;

    /**
     * Constructor.
     *
     * @param context Context object holds the resources which are shared among all the threads
     * @param currentPrepareMessageId Current number part of the Prepare identifier
     * @param proposedPrepareMessageId Identifier of the proposed Prepare message
     */
    public Reject(Context context, long currentPrepareMessageId, String proposedPrepareMessageId) {
        this.messageType = Type.REJECT;

        this.responderNodeId = context.getNodeId();
        this.currentPrepareMessageId = currentPrepareMessageId;
        this.proposedPrepareMessageId = proposedPrepareMessageId;
    }

    // No-arg constructor used by Jackson
    public Reject() {
    }

    public int getResponderNodeId() {
        return responderNodeId;
    }

    public void setResponderNodeId(int responderNodeId) {
        this.responderNodeId = responderNodeId;
    }

    public void setMessageType(Type messageType) {
        this.messageType = messageType;
    }

    public long getCurrentPrepareMessageId() {
        return currentPrepareMessageId;
    }

    public void setCurrentPrepareMessageId(long currentPrepareMessageId) {
        this.currentPrepareMessageId = currentPrepareMessageId;
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
