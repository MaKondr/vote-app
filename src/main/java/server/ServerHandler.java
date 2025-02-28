package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import models.Topic;
import models.Vote;
import models.VotingStore;

import java.io.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = Logger.getLogger(ServerHandler.class.getName());
    private String username;
    private String channelId;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channelId = ctx.channel().id().asLongText();
        logger.info("Client connected: " + ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        logger.info("Received from " + (username != null ? username : "unknown") + ": " + msg);
        String[] parts = msg.trim().split("\\s+");
        String command = parts[0];

        if(command.equals("help")) {
            String helpText = """
                        Available commands:
                        login -u=<username> - Log in with the specified username
                        create topic -n=<topic> - Create a new topic
                        create vote -t=<topic> - Create a new vote in the specified topic
                        view - List all topics
                        view -t=<topic> - List votes in the specified topic
                        view -t=<topic> -v=<vote> - View details of a specific vote
                        vote -t=<topic> -v=<vote> - Vote in the specified vote
                        delete -t=<topic> -v=<vote> - Delete a vote (only by creator)
                        save <filename> - Save all data to a file
                        load <filename> - Load data from a file
                        help - Show this help message
                        exit - Exit the program
                        
                        """;
            ctx.writeAndFlush(helpText);
            return;
        }


        // Проверка логина
        if ("login".equals(command) && parts.length > 1 && parts[1].startsWith("-u=")) {
            username = parts[1].substring(3);
            VotingStore.loggedUsers.put(channelId, username);
            ctx.writeAndFlush("Logged in as " + username + "\n");
            return;
        }
        if (username == null) {
            ctx.writeAndFlush("Please login first\n");
            return;
        }

        // Обработка создания голосования пошагово
        if (VotingStore.voteCreationState.containsKey(channelId)) {
            handleVoteCreation(ctx, msg);
            return;
        }

        // Основные команды
        switch (command) {
            case "create":
                if (parts.length > 2 && "topic".equals(parts[1]) && parts[2].startsWith("-n=")) {
                    String topicName = parts[2].substring(3);
                    if (!VotingStore.topics.containsKey(topicName)) {
                        VotingStore.topics.put(topicName, new Topic(topicName));
                        ctx.writeAndFlush("Topic " + topicName + " created\n");
                    } else {
                        ctx.writeAndFlush("Topic already exists\n");
                    }
                } else if (parts.length > 2 && "vote".equals(parts[1]) && parts[2].startsWith("-t=")) {
                    String topicName = parts[2].substring(3);
                    if (VotingStore.topics.containsKey(topicName)) {
                        VotingStore.voteCreationState.put(channelId, topicName + "|name");
                        ctx.writeAndFlush("Enter vote name: ");
                    } else {
                        ctx.writeAndFlush("Topic does not exist\n");
                    }
                }
                break;

            case "view":
                if (parts.length == 1) {
                    StringBuilder response = new StringBuilder();
                    for (Topic t : VotingStore.topics.values()) {
                        response.append(t.name).append(" (votes=").append(t.votes.size()).append(")\n");
                    }
                    ctx.writeAndFlush(!response.isEmpty() ? response.toString() : "No topics\n");
                } else if (parts[1].startsWith("-t=")) {
                    String topicName = parts[1].substring(3);
                    Topic topic = VotingStore.topics.get(topicName);
                    if (topic != null) {
                        if (parts.length == 2) {
                            StringBuilder response = new StringBuilder("Votes in " + topicName + ":\n");
                            for (String voteName : topic.votes.keySet()) {
                                response.append(voteName).append("\n");
                            }
                            ctx.writeAndFlush(response.length() > 6 ? response.toString() : "No votes\n");
                        } else if (parts[2].startsWith("-v=")) {
                            String voteName = parts[2].substring(3);
                            Vote vote = topic.votes.get(voteName);
                            if (vote != null) {
                                StringBuilder response = new StringBuilder(vote.description + "\n");
                                for (Map.Entry<String, Integer> opt : vote.options.entrySet()) {
                                    response.append(opt.getKey()).append(": ").append(opt.getValue()).append("\n");
                                }
                                ctx.writeAndFlush(response.toString());
                            } else {
                                ctx.writeAndFlush("Vote not found\n");
                            }
                        }
                    } else {
                        ctx.writeAndFlush("Topic not found\n");
                    }
                }
                break;

            case "vote":
                if (parts.length > 2 && parts[1].startsWith("-t=") && parts[2].startsWith("-v=")) {
                    String topicName = parts[1].substring(3);
                    String voteName = parts[2].substring(3);
                    Topic topic = VotingStore.topics.get(topicName);
                    if (topic != null && topic.votes.containsKey(voteName)) {
                        Vote vote = topic.votes.get(voteName);
                        StringBuilder opts = new StringBuilder("Options:\n");
                        for (String opt : vote.options.keySet()) {
                            opts.append(opt).append("\n");
                        }
                        opts.append("Enter your choice: ");
                        VotingStore.voteCreationState.put(channelId, topicName + "|" + voteName + "|voting");
                        ctx.writeAndFlush(opts.toString());
                    } else {
                        ctx.writeAndFlush("Topic or vote not found\n");
                    }
                }
                break;

            case "delete":
                if (parts.length > 2 && parts[1].startsWith("-t=") && parts[2].startsWith("-v=")) {
                    String topicName = parts[1].substring(3);
                    String voteName = parts[2].substring(3);
                    Topic topic = VotingStore.topics.get(topicName);
                    if (topic != null && topic.votes.containsKey(voteName)) {
                        if (topic.votes.get(voteName).creator.equals(username)) {
                            topic.votes.remove(voteName);
                            ctx.writeAndFlush("Vote deleted\n");
                        } else {
                            ctx.writeAndFlush("Only creator can delete this vote\n");
                        }
                    } else {
                        ctx.writeAndFlush("Topic or vote not found\n");
                    }
                }
                break;

            case "save":
                if (parts.length > 1) {
                    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(parts[1]))) {
                        oos.writeObject(VotingStore.topics);
                        ctx.writeAndFlush("Data saved to " + parts[1] + "\n");
                    } catch (IOException e) {
                        ctx.writeAndFlush("Error saving data\n");
                    }
                }
                break;

            case "load":
                if (parts.length > 1) {
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(parts[1]))) {
                        // Безопасное приведение типов требует подавления предупреждения
                        @SuppressWarnings("unchecked")
                        Map<String, Topic> loadedTopics = (Map<String, Topic>) ois.readObject();
                        VotingStore.topics = loadedTopics;
                        ctx.writeAndFlush("Data loaded from " + parts[1] + "\n");
                    } catch (Exception e) {
                        ctx.writeAndFlush("Error loading data\n");
                    }
                }
                break;

            case "exit":
                ctx.close();
                break;

            default:
                ctx.writeAndFlush("Unknown command. Type 'help' for a list of commands.\n");
        }
    }

    private void handleVoteCreation(ChannelHandlerContext ctx, String msg) {
        String state = VotingStore.voteCreationState.get(channelId);
        String[] stateParts = state.split("\\|");
        String topicName = stateParts[0];

        if (state.endsWith("|name")) {
            String voteName = msg.trim();
            if (VotingStore.topics.get(topicName).votes.containsKey(voteName)) {
                ctx.writeAndFlush("Vote name already exists. Enter another: ");
            } else {
                VotingStore.voteCreationState.put(channelId, topicName + "|" + voteName + "|desc");
                ctx.writeAndFlush("Enter vote description: ");
            }
        } else if (state.endsWith("|desc")) {
            String desc = msg.trim();
            VotingStore.voteCreationState.put(channelId, topicName + "|" + stateParts[1] + "|" + desc + "|opts|0");
            ctx.writeAndFlush("Enter number of options: ");
        } else if (state.contains("|opts|")) {
            int optCount = Integer.parseInt(stateParts[4]);
            if (stateParts.length == 5) {
                int newCount = Integer.parseInt(msg.trim());
                VotingStore.voteCreationState.put(channelId, state + "|" + newCount);
                ctx.writeAndFlush("Enter option 1: ");
            } else {
                Vote vote = new Vote(stateParts[1], stateParts[2], username);
                vote.addOption(msg.trim());
                VotingStore.topics.get(topicName).votes.put(stateParts[1], vote);
                if (stateParts.length - 5 < optCount) {
                    ctx.writeAndFlush("Enter option " + (stateParts.length - 4) + ": ");
                    VotingStore.voteCreationState.put(channelId, state + "|" + msg.trim());
                } else {
                    VotingStore.voteCreationState.remove(channelId);
                    ctx.writeAndFlush("Vote created\n");
                }
            }
        } else if (state.contains("|voting")) {
            String voteName = stateParts[1];
            Vote vote = VotingStore.topics.get(topicName).votes.get(voteName);
            if (vote.options.containsKey(msg.trim())) {
                vote.vote(msg.trim());
                VotingStore.voteCreationState.remove(channelId);
                ctx.writeAndFlush("Voted\n");
            } else {
                ctx.writeAndFlush("Invalid option\n");
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        VotingStore.loggedUsers.remove(channelId);
        VotingStore.voteCreationState.remove(channelId);
        logger.info("Client disconnected: " + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.log(Level.SEVERE, "Exception caught in channel " + channelId, cause);
        ctx.close();
    }
}