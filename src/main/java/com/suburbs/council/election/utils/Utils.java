package com.suburbs.council.election.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suburbs.council.election.Member;
import com.suburbs.council.election.messages.Message;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class Utils {

    public static ObjectMapper mapper = new ObjectMapper();

    public static String serialize(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

    public static <T> T deserialize(String message, Class<T> classType) throws JsonProcessingException {
        return mapper.readValue(message, classType);
    }

    public static Message.Type getMessageType(String message) throws JsonProcessingException {
        Map<String, Object> map = deserialize(message, Map.class);
        return Message.Type.valueOf((String) map.get("messageType"));
    }

    public static long parsePrepareNumer(String prepareNumber) {
        return (long) Double.parseDouble(prepareNumber);
    }

    public static String generatePrepareNumber(long lastPrepareMessageNumber, int proposerNodeId) {
        return (lastPrepareMessageNumber + 1) + "." + proposerNodeId;
    }

    public static void dispatch(Member member, Object object) throws IOException {
        String message = mapper.writeValueAsString(object);

        if (!member.isConnected())
            member.initializeSocket();

        Socket socket = member.socket();
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println(message);
        out.close();
    }
}
