package models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VotingStore {
    public static Map<String, Topic> topics = new ConcurrentHashMap<>();
    public static Map<String, String> loggedUsers = new ConcurrentHashMap<>(); // канал -> юзер
    public static Map<String, String> voteCreationState = new ConcurrentHashMap<>(); // канал -> состояние создания голосования
}