package com.lukaswillsie.onlinechess.network.threads;

import java.io.DataInputStream;
import java.io.EOFException;
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
     * Creates a new NetworkThread that will use the given devices to read from and write to the
     * server
     *
     * @param writer - the device that this NetworkThread will use to write to the server
     * @param reader - the device that this NetworkThread will use to read from the server
     */
    public NetworkThread(PrintWriter writer, DataInputStream reader) {
        this.writer = writer;
        this.reader = reader;
    }

    /**
     * Read a single integer from the server and return it
     *
     * @return the integer read from the server
     * @throws EOFException    if the server has willfully closed its connection with us when the read
     *                         occurs
     * @throws SocketException if the connection with the server has been closed for some other
     *                         reason, for example if the server crashed
     * @throws IOException     if there is some other problem with the read, like a system error
     */
    int readInt() throws EOFException, SocketException, IOException {
        return reader.readInt();
    }

    /**
     * Reads a single line of input from the server. That is, reads ONE-BYTE chars from the server
     * repeatedly until a network newline, "\r\n", is found.
     *
     * @throws SocketException if the server has disconnected when this method tries to read from it
     * @throws IOException     if there is some other problem with the read, like a system error
     */
    String readLine() throws SocketException, IOException {
        char[] last = {'\0', '\0'};
        StringBuilder builder = new StringBuilder();

        char read;
        while (last[0] != '\r' || last[1] != '\n') {
            read = (char) reader.read();
            last[0] = last[1];
            last[1] = read;

            builder.append(read);
        }

        // Truncate the builder to omit the "\r\n" at the end of the line
        builder.setLength(builder.length() - 2);
        return builder.toString();
    }

    /**
     * Send the given request to the server.
     */
    void sendRequest(String request) {
        writer.println(request);
    }
}
