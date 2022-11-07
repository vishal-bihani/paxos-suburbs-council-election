package com.suburbs.council.election.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suburbs.council.election.Member;
import com.suburbs.council.election.messages.Message;
import com.suburbs.council.election.paxos.Candidate;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

/**
 * Utils class for Paxos related operation.
 */
public class PaxosUtils {

    public static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Implicit constructor to hide the public one
     */
    private PaxosUtils() {
    }

    /**
     * Serializes the given object into JSON.
     *
     * @param object Object to serialize
     * @return JSON string
     * @throws JsonProcessingException Throws if encounters error while serialization
     */
    public static String serialize(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

    /**
     * Deserializes the JSON string to given Class type.
     *
     * @param message JSON string to deserialize
     * @param classType Class type
     * @return Object of given class type
     * @param <T> Class type
     * @throws JsonProcessingException Throws if encounters error while deserialization
     */
    public static <T> T deserialize(String message, Class<T> classType) throws JsonProcessingException {
        return mapper.readValue(message, classType);
    }

    /**
     * Returns the type of {@link Message}.
     *
     * @param message JSON String
     * @return Type of Message implementation
     * @throws JsonProcessingException Throws if encounters error while deserialization
     */
    public static Message.Type getMessageType(String message) throws JsonProcessingException {
        Map<String, Object> map = deserialize(message, Map.class);

        return Message.Type
                .valueOf((String) map.get(Message.MESSAGE_TYPE_KEY));
    }

    /**
     * The PREPARE message identifiers are string values with the following format:
     * <pre><code>number.nodeId</code></pre>
     * <br>
     * This method parse the identifier and returns the number.
     *
     * @param prepareNumber PREPARE message identifier
     * @return Number part of the identifier
     */
    public static long parsePrepareNumer(String prepareNumber) {
        return (long) Double.parseDouble(prepareNumber);
    }

    /**
     * This generates higher identifier than existing one for PREPARE message. It will
     * add random number as per {@link com.suburbs.council.election.paxos.Candidate#MAX_PREPARE_ID_ADD}
     * and {@link com.suburbs.council.election.paxos.Candidate#MIN_PREPARE_ID_ADD}.
     *
     * @param lastPrepareMessageNumber Number of last PREPARE message
     * @param proposerNodeId Id of the current node
     * @return Formatted identifier for PREPARE message
     */
    public static String generatePrepareNumber(long lastPrepareMessageNumber, int proposerNodeId) {
        int salt = generateRandomNumber(Candidate.MAX_PREPARE_ID_ADD, Candidate.MIN_PREPARE_ID_ADD);
        return (lastPrepareMessageNumber + salt) + "." + proposerNodeId;
    }

    /**
     * Dispatch the given Object to given {@link Member}. This will first serialize
     * the object into JSON string and then send over the socket.
     *
     * @param member Member to whom this object has to be dispatched
     * @param object Object to dispatch
     * @throws IOException Throws if encounters any error while sending data to the member over socket
     */
    public static void dispatch(Member member, Object object) throws IOException {
        String message = mapper.writeValueAsString(object);

        // Check if the socket not connected
        if (!member.isConnected())
            member.initializeSocket(); // Attempt to reconnect to the socket

        Socket socket = member.socket();
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(message);
        out.close(); // This will release the resources
    }

    /**
     * Generates random number in the given range.
     *
     * @param max Max
     * @param min Min
     * @return Random number
     */
    public static int generateRandomNumber(int max, int min) {
        return (int) ((Math.random() * (max - min)) + min);
    }
}
