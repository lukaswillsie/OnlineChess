package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;
import android.util.Log;

import com.lukaswillsie.onlinechess.network.ReturnCodes;
import com.lukaswillsie.onlinechess.network.threads.ReturnCodeCaller;
import com.lukaswillsie.onlinechess.network.threads.ReturnCodeThread;


class CreateAccountHelper extends SubHelper implements ReturnCodeCaller {
    /**
     * Tag for logging to the console
     */
    private static final String tag = "CreateAccountHelper";
    private CreateAccountRequester requester;

    /**
     * Constants used by this object to communicate with itself through Messages
     */
    private static final int SYSTEM_ERROR = -3;
    private static final int CONNECTION_LOST = -2;
    private static final int SERVER_ERROR = -1;
    private static final int SUCCESS = 0;
    private static final int USERNAME_IN_USE = 1;
    private static final int ACCOUNT_FORMAT_INVALID = 2;

    /**
     * Create a new SubHelper as part of the given ServerHelper
     *
     * @param container - the ServerHelper that this object is a part of
     */
    CreateAccountHelper(ServerHelper container) {
        super(container);
    }

    void createAccount(CreateAccountRequester requester, String username, String password) {
        this.requester = requester;

        ReturnCodeThread thread = new ReturnCodeThread(this.getRequest(username, password), this);
        thread.setReader(this.getIn());
        thread.setWriter(this.getOut());
        thread.start();
    }

    /**
     * Returns a String that can be sent to the server as a request to create an account with the
     * given username and password.
     *
     * @param username - the username for the account to be created
     * @param password - the password for the account to be created
     * @return a String, representing an account creation request that can be sent directly to the
     * server
     */
    private String getRequest(String username, String password) {
        return "create " + username + " " + password;
    }

    /**
     * This method is called by the ReturnCodeThread that this object starts to notify it of the
     * server's response to its request.
     *
     * @param code - the code returned by the server
     */
    @Override
    public void onServerReturn(int code) {
        Message msg;
        switch (code) {
            // If the server is telling us we formatted our request incorrectly
            case ReturnCodes.FORMAT_INVALID:
                Log.i(tag, "Create account request returned format invalid. Make sure " +
                        "request format conforms to protocol");
                // We log the problem, but in the running of the program treat this as a server
                // error. This should never happen; our requests are formatted exactly the
                // way the server is expecting them, and if this does happen it's a sign that we've
                // messed up somewhere. Regardless, we have to have a way to handle it at runtime,
                // and we choose to handle it as a server error.
                msg = this.obtainMessage(SERVER_ERROR);
                break;
            // If the server is telling us an error occurred
            case ReturnCodes.SERVER_ERROR:
                Log.i(tag, "Server error occurred during create account request");
                msg = this.obtainMessage(SERVER_ERROR);
                break;
            // If the server is telling us we successfully created the account
            case ReturnCodes.Create.SUCCESS:
                Log.i(tag, "Account successfully created");
                msg = this.obtainMessage(SUCCESS);
                break;
            // If the server is telling us the desired username is in use
            case ReturnCodes.Create.USERNAME_IN_USE:
                Log.i(tag, "Username is already in use");
                msg = this.obtainMessage(USERNAME_IN_USE);
                break;
            // If the server is telling us that the username/password is formatted unacceptably
            case ReturnCodes.Create.FORMAT_INVALID:
                Log.i(tag, "Username/password incorrectly formatted");
                msg = this.obtainMessage(ACCOUNT_FORMAT_INVALID);
                break;
            // The cases we enumerated above are exhaustive, so any other result falls outside of
            // what the server has said it might return, according to protocol, and is treated by
            // this program as an error server-side.
            default:
                Log.i(tag, "Unfamiliar return code " + code + " returned by server");
                msg = this.obtainMessage(SERVER_ERROR);
                break;
        }

        msg.sendToTarget();
    }

    /**
     * Is called if a problem with the system is encountered during execution of a client Thread.
     * For example, an exception that is unrelated to the server might be thrown by the operating
     * system when a Thread tries to write/read from the socket. We allow the caller, and therefore
     * the front-end of the app, to differentiate between this kind of error and a server error,
     */
    @Override
    public void systemError() {
        this.obtainMessage(SYSTEM_ERROR).sendToTarget();
    }

    /**
     * To be called if a client Thread discovers the server to have disconnected in the course of
     * its work.
     */
    @Override
    public void connectionLost() {
        this.notifyContainerConnectionLost();
        this.obtainMessage(CONNECTION_LOST).sendToTarget();
    }

    /**
     * We use this method to communicate events pertaining to an active account creation request to
     * the UI thread. Note that this object only receives messages from itself.
     *
     * @param msg - the received message
     */
    @Override
    public void handleMessage(Message msg) {
        switch(msg.what) {
            case SYSTEM_ERROR:
                requester.systemError();
                break;
            case CONNECTION_LOST:
                requester.connectionLost();
                break;
            case SERVER_ERROR:
                requester.serverError();
                break;
            case SUCCESS:
                requester.createAccountSuccess();
                break;
            case USERNAME_IN_USE:
                requester.usernameInUse();
                break;
            case ACCOUNT_FORMAT_INVALID:
                requester.formatInvalid();
                break;
        }
    }
}
