package com.suburbs.council.election.messages;

/**
 * HeartBeat messages let members know that the node is still up and running.
 */
public class HeartBeat implements Message {

    private String name;
    private Type messageType;

    /**
     * Constructor.
     *
     * @param name Node name
     */
    public HeartBeat(String name) {
        this.name = name;
        this.messageType = Type.HEARTBEAT;
    }

    /**
     * Constructor.
     *
     * @param name Node name
     */
    public HeartBeat(String name, Type messageType) {
        this.name = name;
        this.messageType = messageType;
    }

    // Used by jackson
    public HeartBeat() {
    }

    /**
     * Returns the node name.
     *
     * @return node name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the current node.
     *
     * @param name Name of the node
     */
    public void setName(String name) {
        this.name = name;
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

    /**
     * Set the type of the message.
     *
     * @return Type of the message
     */
    public void setMessageType(Type messageType) {
        this.messageType = messageType;
    }
}
