package Server;

import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    public static List<ClientHandler> clientHandlers = new ArrayList<>();
    private static List<String> last_messages = new ArrayList<>();
    private Socket socket;
    private BufferedWriter bufferedWriter;
    private BufferedReader bufferedReader;
    private String name;
    private DataOutputStream dataOutputStream = null;
    private DataInputStream dataInputStream = null;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            this.name = bufferedReader.readLine();
            clientHandlers.add(this);
            shout("\u001b[38;5;47m" + "Server: " + name + " joined!" + "\u001b[0m");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void popLastMessages() {
        try {
            this.bufferedWriter.write("Last 3 messages:");
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String s : last_messages) {
            try {
                this.bufferedWriter.write(s);
                this.bufferedWriter.newLine();
                this.bufferedWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void shout(String msg) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.name.equals(name)) {
                    clientHandler.bufferedWriter.write(msg);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                System.out.println("Exception while shouting");
                removeClient();
            }
        }
        if (!msg.contains("joined!") && !msg.contains("left!"))
            last_messages.add(msg);
        if (last_messages.size() > 3)
            last_messages.removeLast();
    }

    public void removeClient() {
        if (true)
            return;
        clientHandlers.remove(this);
        try {
            bufferedWriter.close();
            bufferedReader.close();
            socket.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void showFiles() {
        try {
            File folder = new File("data");
            File[] listOfFiles = folder.listFiles();
            String files = "";
            if (listOfFiles != null) {
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isFile()) {
                        files = files + (i + 1 + ". " + listOfFiles[i].getName()) + "\n";
                    }
                }
            }
            this.bufferedWriter.write(files + "END");
            this.bufferedWriter.newLine();
            this.bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getFile(int id) throws IOException {
        File folder = new File("data");
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles.length <= id)
            return;
        File file = listOfFiles[id - 1];
        int bytes = 0;
        // Open the File where he located in your pc
        FileInputStream fileInputStream
                = new FileInputStream(file);
        // Here we send the File to Client
        dataOutputStream.writeLong(file.length());
        // Here we  break file into chunks
        byte[] buffer = new byte[4 * 1024];
        while ((bytes = fileInputStream.read(buffer))
                != -1) {
            // Send the file to Server Socket
            dataOutputStream.write(buffer, 0, bytes);
            dataOutputStream.flush();
        }
        // close the file here
        fileInputStream.close();
    }

    @Override
    public void run() {
        while (socket.isConnected()) {
            try {
                String query = bufferedReader.readLine();
                JSONObject jsonQuery = new JSONObject(query);
                String status = jsonQuery.getString("status");
                if (status.equals("message"))
                    shout(jsonQuery.getString("text"));
                else if (status.equals("exit_chat")) {
                    shout(jsonQuery.getString("text"));
                    removeClient();
                } else if (status.equals("join_chat")) {
                    popLastMessages();
                } else if (status.equals("show_files"))
                    showFiles();
                else if (status.equals("get_file"))
                    getFile(jsonQuery.getInt("text"));
            } catch (IOException e) {
                removeClient();
                break;
            }
        }
    }


}
