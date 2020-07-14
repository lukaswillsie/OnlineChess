package com.lukaswillsie.onlinechess.network.helper;

/**
 * A thread-safe implementation of a Request queue using a linked list with head and tail pointers
 */
public class RequestQueue {
    /*
     * Pointers to beginning and end of the list, respectively
     */
    private Node head;
    private Node tail;

    /**
     * Return a reference to the first element in the queue, without removing it, or null if the
     * list is empty.
     *
     * @return the first request object in this queue
     */
    public synchronized Request getHead() {
        if (head == null) {
            return null;
        } else {
            return head.request;
        }

    }

    /**
     * Add the given request to the back of the queue
     *
     * @param request - the request to append to the queue
     */
    public synchronized void enqueue(Request request) {
        if (head == null) {
            head = new Node(request);
            tail = head;
        } else {
            Node last = new Node(request);
            tail.next = last;
            tail = last;
        }
    }

    /**
     * Remove the request at the head of the queue and return it. Returns null if the queue is emtpy
     *
     * @return the head of the queue
     */
    public synchronized Request dequeue() {
        if (head == null) {
            return null;
        }

        Node first = head;
        // If there's only one element in the queue, reassign tail to be null
        if (head.next == null) {
            tail = null;
        }
        head = head.next;

        return first.request;
    }

    /**
     * A Node in a linked list
     */
    private static class Node {
        private Node next;
        private Request request;

        private Node(Request request) {
            this.request = request;
        }
    }
}
