package models;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Topic implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public String name;
    public Map<String, Vote> votes;

    public Topic(String name) {
        this.name = name;
        this.votes = new HashMap<>();
    }
}