package models;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Vote implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public String name;
    public String description;
    public Map<String, Integer> options;
    public String creator;

    public Vote(String name, String description, String creator) {
        this.name = name;
        this.description = description;
        this.options = new HashMap<>();
        this.creator = creator;
    }

    public void addOption(String option) {
        options.put(option, 0);
    }

    public void vote(String option) {
        if (options.containsKey(option)) {
            options.put(option, options.get(option) + 1);
        }
    }
}