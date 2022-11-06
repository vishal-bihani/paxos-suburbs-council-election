package com.suburbs.council.election;

import com.suburbs.council.election.enums.Profile;
import com.suburbs.council.election.enums.ResponseTiming;
import java.util.List;

/**
 * This class holds the configurations of the current node.
 */
public class Node {

    private int id;

    private String name;

    private int rank;

    private int port;

    private ResponseTiming responseTiming;

    private Profile profile;

    private List<Member> members;

    private int initProposeDelay;

    public Node(int id, String name, int rank, ResponseTiming responseTiming,
                Profile profile, List<Member> members,
                int initProposeDelay) {

        this.id = id;
        this.name = name;
        this.rank = rank;
        this.responseTiming = responseTiming;
        this.profile = profile;
        this.members = members;
        this.initProposeDelay = initProposeDelay;
    }

    // No-Arg constructor for jackson library
    public Node() {
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

    public ResponseTiming getResponseTiming() {
        return responseTiming;
    }

    public void setResponseTiming(ResponseTiming responseTiming) {
        this.responseTiming = responseTiming;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members;
    }

    public int getInitProposeDelay() {
        return initProposeDelay;
    }

    public void setInitProposeDelay(int initProposeDelay) {
        this.initProposeDelay = initProposeDelay;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "NodeConfiguration{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", rank=" + rank +
                ", responseTiming=" + responseTiming +
                ", profile=" + profile +
                ", port=" + port +
                ", memberConfiguration=" + members +
                ", initProposeDelay=" + initProposeDelay +
                '}';
    }
}
