package com.suburbs.council.election.messages;

import java.io.Serializable;

public class Proposal implements Serializable {

    private String proposedMessage;

    public Proposal(String proposedMessage) {
        this.proposedMessage = proposedMessage;
    }

    public Proposal() {
    }

    public String getProposedMessage() {
        return proposedMessage;
    }

    public void setProposedMessage(String proposedMessage) {
        this.proposedMessage = proposedMessage;
    }
}
