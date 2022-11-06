package com.suburbs.council.election.messages;

import com.suburbs.council.election.paxos.Context;

/**
 * Accepted message is sent by the Member back to the Proposer
 * indicating that they have accepted the proposed state.
 */
public class Accepted implements Message {

    private Type messageType;
    private int responderNodeId;
    private String responderNodeName;
    private String prepareMessageId;
    private Prepare proposedPrepareMessage;

    /**
     * Constructor.
     *
     * @param context Context object holds the resources which are shared among all the threads
     * @param prepareMessageId Identifier of the PREPARE message
     * @param proposedPrepareMessage Original {@link Prepare} message with the {@link Proposal}
     */
    public Accepted(Context context, String prepareMessageId, Prepare proposedPrepareMessage) {
        this.messageType = Type.ACCEPTED;

        this.responderNodeId = context.getNodeId();
        this.responderNodeName = context.getNodeName();
        this.prepareMessageId = prepareMessageId;
        this.proposedPrepareMessage = proposedPrepareMessage;
    }

    // No-args constructor used by Jackson
    public Accepted() {
    }

    public int getResponderNodeId() {
        return responderNodeId;
    }

    public void setResponderNodeId(int responderNodeId) {
        this.responderNodeId = responderNodeId;
    }

    public String getResponderNodeName() {
        return responderNodeName;
    }

    public void setResponderNodeName(String responderNodeName) {
        this.responderNodeName = responderNodeName;
    }

    public void setMessageType(Type messageType) {
        this.messageType = messageType;
    }

    public String getPrepareMessageId() {
        return prepareMessageId;
    }

    public void setPrepareMessageId(String prepareMessageId) {
        this.prepareMessageId = prepareMessageId;
    }

    public Prepare getProposedPrepareMessage() {
        return proposedPrepareMessage;
    }

    public void setProposedPrepareMessage(Prepare proposedPrepareMessage) {
        this.proposedPrepareMessage = proposedPrepareMessage;
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
