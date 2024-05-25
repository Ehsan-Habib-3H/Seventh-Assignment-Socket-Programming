package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

// Server Class
public class Server {
    private ServerSocket serverSocket;
    public Server(ServerSocket serverSocket)
    {
        this.serverSocket = serverSocket;
    }
    public void startServer(){
        try {
            System.out.println("\u001b[38;5;14m"+"Server started (;" + "\u001b[0m");
            while (true)
            {
                Socket socket = serverSocket.accept();
//                System.out.println("New client connected!");
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        server.startServer();

    }
}