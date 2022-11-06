package com.suburbs.council.election;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * This class holds the configurations for each Member node.
 */
public class Member {

    private int id;

    private String name;

    private int rank;

    private String host;

    private int port;

    private Socket socket;

    // Initially all members are inactive
    private boolean isActiveMember = false;


    /**
     * It will create {@link SocketAddress} with the given {@link #host} and {@link #port}
     * and will initialize {@link #socket}. Once instantiated it will attempt to connect to
     * the socket address.
     *
     * @throws IOException Thrown if something goes wrong while attempting to connect
     *                      to the socket address
     */
    public void initializeSocket() throws IOException {

        // If socket is null, initialize SocketAddress and Socket
        if (isConnected()) {
            return;
        }

        SocketAddress socketAddress = new InetSocketAddress(host, port);
        socket = new Socket();
        socket.connect(socketAddress);
        setActiveMember(true);
    }

    public Member(int id, String name, int rank, String host, int port) {

        this.id = id;
        this.name = name;
        this.rank = rank;
        this.host = host;
        this.port = port;
    }

    // No-Arg constructor for jackson library
    public Member() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setActiveMember(boolean isActiveMember) {
        this.isActiveMember = isActiveMember;
    }

    public boolean isActiveMember() {
        return this.isActiveMember;
    }

    public boolean isConnected() {
        return (socket != null && socket.isConnected()) && !socket.isClosed();
    }

    public Socket socket() {
        return socket;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "MemberConfiguration{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", rank=" + rank +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
