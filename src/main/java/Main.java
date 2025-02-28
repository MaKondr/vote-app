import client.Client;
import server.Server;


public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || "server".equals(args[0])) {
            new Server(8080).run();
        } else if ("client".equals(args[0])) {
            new Client("localhost", 8080).run();
        } else {
            System.out.println("Use: java VotingApp [server|client]");
        }
    }
}
