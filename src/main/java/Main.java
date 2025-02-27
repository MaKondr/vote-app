import client.Client;
import server.Server;
public class Main {
    public static void main(String[] args) {
        String lesomIdite = "Lesom idite";
        if(args.length != 1) {
            System.out.println(lesomIdite);
            return;
        }
        switch (args[0]){
            case "client":
                Client.start(); //start client
                break;
            case "server":
                Server.start(); //start server
                break;

            default:
                System.out.println(lesomIdite);
        }
    }
}
