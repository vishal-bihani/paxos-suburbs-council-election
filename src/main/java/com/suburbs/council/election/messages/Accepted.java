package com.suburbs.council.election.messages;

import com.suburbs.council.election.paxos.Context;

public class Accepted implements Message {

    private Type messageType;
    private int responderNodeId;
    private String responderNodeName;
    private String prepareMessageId;
    private Prepare proposedPrepareMessage;

    public Accepted(Context context, String prepareMessageId, Prepare proposedPrepareMessage) {
        this.messageType = Type.ACCEPTED;

        this.responderNodeId = context.getNodeId();
        this.responderNodeName = context.getNodeName();
        this.prepareMessageId = prepareMessageId;
        this.proposedPrepareMessage = proposedPrepareMessage;
    }

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
