package com.suburbs.council.election.messages;

import com.suburbs.council.election.paxos.Context;

/**
 * Accept messages are sent by Proposer requesting other members
 * to fulfill their Promise, accept the proposed state and come to consensus.
 */
public class Accept implements Message {

    private Type messageType;
    private int proposerNodeId;
    private String proposerNodeName;
    private String prepareMessageId;

    /**
     * Constructor.
     *
     * @param context Context object holds the resources which are shared among all the threads
     * @param prepareMessageId Identifier of the PREPARE message
     */
    public Accept(Context context, String prepareMessageId) {
        this.messageType = Type.ACCEPT;

        this.proposerNodeId = context.getNodeId();
        this.proposerNodeName = context.getNodeName();
        this.prepareMessageId = prepareMessageId;
    }

    // No-Arg constructor Used by the Jackson
    public Accept() {
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

    public int getProposerNodeId() {
        return proposerNodeId;
    }

    public void setProposerNodeId(int proposerNodeId) {
        this.proposerNodeId = proposerNodeId;
    }

    public String getProposerNodeName() {
        return proposerNodeName;
    }

    public void setProposerNodeName(String proposerNodeName) {
        this.proposerNodeName = proposerNodeName;
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
