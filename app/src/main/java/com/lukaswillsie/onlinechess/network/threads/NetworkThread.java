package com.lukaswillsie.onlinechess.network.threads;

import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;

/**
 * This class centralizes functionality common to all types of Threads in our app.
 */
abstract class NetworkThread extends Thread {
    /*
     * The IO devices this object will use to communicate with the server. MUST BE SET through
     * setter methods below before the thread is started
     */
    private PrintWriter writer;
    private DataInputStream reader;

    /**
     * Give this NetworkThread a PrintWriter to use to write to the server
     * @param writer - the PrintWriter this thread should use to write to the server
     */
    public void setWriter(PrintWriter writer) {
        this.writer = writer;
    }

    /**
     * Give this NetworkThread a DataInputStream to use to read from the server
     * @param reader - the DataInputStream this thread should use to read from the server
     */
    public void setReader(DataInputStream reader) {
        this.reader = reader;
    }


    /**
     * Read a single integer from the server and return it
     * @return the integer read from the server
     * @throws SocketException if the server has disconnected when this method tries to read from it
     * @throws IOException if there is some other problem with the read, like a system error
     */
    int readInt() throws SocketException, IOException {
        // TODO: Look into what happens when the server shuts down and the reader tries to read
        return reader.readInt();
    }

    /**
     * Reads a single line of input from the server. That is, reads ONE-BYTE chars from the server
     * repeatedly until a network newline, "\r\n", is found.
     *
     * @throws SocketException if the server has disconnected when this method tries to read from it
     * @throws IOException if there is some other problem with the read, like a system error
     */
    String readLine() throws SocketException, IOException {
        char[] last = {'\0', '\0'};
        StringBuilder builder = new StringBuilder();

        char read;
        while(last[0] != '\r'|| last[1] != '\n') {
            read = (char)reader.read();
            last[0] = last[1];
            last[1] = read;

            builder.append(read);
        }

        // Truncate the builder to omit the "\r\n" at the end of the line
        builder.setLength(builder.length() - 2);
        String line = builder.toString();
        return line;
    }

    /**
     * Send the given request to the server.
     */
    void sendRequest(String request) {
        writer.println(request);
    }
}