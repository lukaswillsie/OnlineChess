package com.lukaswillsie.onlinechess.network.helper;

/**
 * A thread-safe implementation of a queue using a doubly-linked list
 */
public class RequestQueue {
    private static class Node {
        private Node next;
        private Request request;

        private Node(Request request) {
            this.request = request;
        }
    }

    /*
     * Pointers to beginning and end of the list, respectively
     */
    private Node head;
    private Node tail;

    public synchronized Request getHead() {
        if(head == null) {
            return null;
        }
        else {
            return head.request;
        }

    }

    public synchronized void enqueue(Request request) {
        if(head == null) {
            head = new Node(request);
            tail = head;
        }
        else {
            Node last = new Node(request);
            tail.next = last;
            tail = last;
        }
    }

    public synchronized Request dequeue() {
        if(head == null) {
            return null;
        }

        Node first = head;
        // If there's only one element in the queue, reassign tail to be null
        if(head.next == null) {
            tail = null;
        }
        head = head.next;

        return first.request;
    }
}
