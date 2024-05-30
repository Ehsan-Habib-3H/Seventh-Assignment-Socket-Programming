package Client;

import netscape.javascript.JSObject;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

public class Client {

    Socket socket;
    private boolean killThread = false;
    InputStreamReader inputStreamReader;
    OutputStreamWriter outputStreamWriter;
    BufferedReader bufferedReader;
    BufferedWriter bufferedWriter;
    private DataOutputStream dataOutputStream = null;
    private DataInputStream dataInputStream = null;
    String name;
    Thread listenThread;
    Thread sendThread;
    JSONObject jsonQuery;

    public Client(Socket socket, String name) {
        try {
            this.socket = socket;
            inputStreamReader = new InputStreamReader(socket.getInputStream());
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
            bufferedReader = new BufferedReader(inputStreamReader);
            bufferedWriter = new BufferedWriter(outputStreamWriter);
            this.name = name;
            // Tell the server my name
            bufferedWriter.write(name);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void joinChat() {
        jsonQuery.put("status", "join_chat");
        try {
            bufferedWriter.write(jsonQuery.toString());
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send() {
        sendThread = new Thread(() -> {
            try {
                Scanner scanner = new Scanner(System.in);
                while (socket.isConnected()) {
                    String msg = scanner.nextLine();
                    if (!msg.equals("exit")) {
                        jsonQuery.put("status", "message");
                        jsonQuery.put("text", "\u001b[38;5;216m" + name + "\u001b[0m" + ": " + msg);
                        bufferedWriter.write(jsonQuery.toString());
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                    } else {
                        jsonQuery.put("status", "exit_chat");
                        jsonQuery.put("text", "\u001b[38;5;160m" + name + " left!" + "\u001b[0m");
                        bufferedWriter.write(jsonQuery.toString());
                        bufferedWriter.newLine();
                        bufferedWriter.flush();
                        killThread = true;
                        listenThread.interrupt();
                        return;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        sendThread.start();

    }

    public void listen() {
        listenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                while (socket.isConnected()) {
                    if (killThread)
                        return;
                    try {
                        msg = bufferedReader.readLine();
                        System.out.println(msg == null ? "" : msg);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        listenThread.start();
    }

    private void showFiles() throws IOException {
        jsonQuery.put("status", "show_files");
        try {
            bufferedWriter.write(jsonQuery.toString());
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            String line;
            while (!Objects.equals(line = bufferedReader.readLine(), "END")) {
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Finished");
        System.out.println("0. back");
        Scanner scanner = new Scanner(System.in);
        String id = scanner.nextLine();
        if (id.equals("0"))
            return;
        jsonQuery.put("status", "get_file");
        jsonQuery.put("text", id);
        try {
            bufferedWriter.write(jsonQuery.toString());
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Receiving file
        int bytes = 0;
        FileOutputStream fileOutputStream
                = new FileOutputStream("clientData.txt");

        long size
                = dataInputStream.readLong(); // read file size
        byte[] buffer = new byte[4 * 1024];
        while (size > 0
                && (bytes = dataInputStream.read(
                buffer, 0,
                (int) Math.min(buffer.length, size)))
                != -1) {
            // Here we write the file using write method
            fileOutputStream.write(buffer, 0, bytes);
            size -= bytes; // read upto file size
        }
        // Here we received file
        System.out.println("File is Received");
        fileOutputStream.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your name: ");
        String name = scanner.nextLine();
        System.out.println("What do you wanna do?\n1. Chatroom\n2. Files");
        String input = scanner.nextLine();
        Socket socket = null;
        try {
            socket = new Socket("localhost", 1234);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Client client = new Client(socket, name);
        client.jsonQuery = new JSONObject();
        while (true) {
            if (input.equals("1")) {
                client.killThread = false;
                client.send();
                client.listen();
                client.joinChat();
                client.sendThread.join();
                client.listenThread.interrupt();
                client.listenThread.join();
            } else if (input.equals("2")) {
                client.showFiles();
            }
            System.out.println("What do you wanna do?\n1. Chatroom\n2. Files");
            input = scanner.nextLine();
        }
    }
}