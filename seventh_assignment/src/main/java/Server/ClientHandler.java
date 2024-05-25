package Server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    public static List<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private String name;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.name = bufferedReader.readLine();
            clientHandlers.add(this);
            shout("\u001b[38;5;47m"+"Server: " + name + " joined!"+"\u001b[0m");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void shout(String msg) {
        for(ClientHandler clientHandler : clientHandlers)
        {
            try{
                if(!clientHandler.name.equals(name))
                {
                    clientHandler.bufferedWriter.write(msg);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                removeClient();
            }
        }
    }
    public void removeClient()
    {
        clientHandlers.remove(this);
        shout("\u001b[38;5;160m"+"Server: "+ name + "left!"+"\u001b[0m");
        try {
            bufferedWriter.close();
            bufferedReader.close();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public void run() {
        while (socket.isConnected()) {
            try {
                String msg = bufferedReader.readLine();
                shout(msg);
            } catch (IOException e) {
                removeClient();
            }
        }
    }
}
