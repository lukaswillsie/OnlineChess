package com.lukaswillsie.onlinechess.network.helper;

import android.os.Message;

import androidx.annotation.NonNull;

import com.lukaswillsie.onlinechess.data.Game;
import com.lukaswillsie.onlinechess.network.helper.requesters.LoginRequester;
import com.lukaswillsie.onlinechess.network.threads.callers.LoginCaller;
import com.lukaswillsie.onlinechess.network.threads.LoginThread;

import java.util.List;

/**
 * A class that makes up part of a ServerHelper façade. Is delegated to for the handling of login
 * requests.
 *
 * Note that this class extends SubHelper, which extends Handler, so this class is a Handler. This
 * is because this object receives callbacks from LoginThread, which will always be running on a
 * Thread other than the UI thread. So when the LoginThread tells us that the login was successful,
 * for example, we won't be running on the UI thread and so can't just call
 * requester.loginSuccessful(), because the UI thread isn't thread-safe. So we as the LoginHelper
 * need a way to get back onto the UI thread to notify the requester when it's time to do so. We use
 * Handlers and Messages to do this.
 */
public class LoginHelper extends SubHelper implements LoginCaller {
    /**
     * The object that made the current request. This is the object we report back news of the
     * request to.
     */
    private LoginRequester requester;

    /**
     * Constants used by this object to communicate with itself through Messages
     */
    private static final int SYSTEM_ERROR = -3;
    private static final int CONNECTION_LOST = -2;
    private static final int SERVER_ERROR = -1;
    private static final int LOGIN_SUCCESS = 0;
    private static final int USERNAME_INVALID = 1;
    private static final int PASSWORD_INVALID = 2;
    private static final int LOGIN_COMPLETE = 3;

    /**
     * Create a LoginHelper object as part of the given ServerHelper façade.
     * @param container
     */
    LoginHelper(ServerHelper container) {
        super(container);
    }

    /**
     * Process a login request using the given credentials, with requester accepting callbacks as to
     * the state of the request.
     *
     * @param requester - the Activity making the login request
     * @param username - the username of the user logging in
     * @param password - the password of the user logging in
     */
    void login(LoginRequester requester, String username, String password) {
        this.requester = requester;

        LoginThread thread = new LoginThread(username, password, this);
        thread.setReader(this.getIn());
        thread.setWriter(this.getOut());
        thread.start();
    }

    /**
     * To be called if, in the midst of the login call, the server returns -1, indicating that it
     * encountered an error. Can also be called if the server returns something that does not
     * conform to protocol.
     */
    @Override
    public void serverError() {
        this.notifyContainerRequestOver();

        Message message = this.obtainMessage(SERVER_ERROR);
        message.sendToTarget();
    }

    /**
     * Is called if a problem with the system is encountered during execution of a client Thread.
     * For example, an exception that is unrelated to the server might be thrown by the operating
     * system when a Thread tries to write/read from the socket. We allow the caller, and therefore
     * the front-end of the app, to differentiate between this kind of error and a server error,
     */
    @Override
    public void systemError() {
        this.notifyContainerRequestOver();

        Message message = this.obtainMessage(SYSTEM_ERROR);
        message.sendToTarget();
    }

    /**
     * Is called if a client Thread discovers the server to have disconnected in the course of
     * its work.
     */
    @Override
    public void connectionLost() {
        // Notify the containing ServerHelper that the connection has been lost
        this.notifyContainerConnectionLost();
        this.notifyContainerRequestOver();

        Message message = this.obtainMessage(CONNECTION_LOST);
        message.sendToTarget();
    }

    /**
     * Is called immediately once the server has confirmed the success of the login request
     */
    @Override
    public void loginSuccess() {
        Message message = this.obtainMessage(LOGIN_SUCCESS);
        message.sendToTarget();
    }

    /**
     * Is called immediately once the server has responded that the given username does not exist
     */
    @Override
    public void usernameInvalid() {
        this.notifyContainerRequestOver();

        Message message = this.obtainMessage(USERNAME_INVALID);
        message.sendToTarget();
    }

    /**
     * Is called immediately once the server has responded that the given password is invalid for
     * the given username
     */
    @Override
    public void passwordInvalid() {
        this.notifyContainerRequestOver();

        Message message = this.obtainMessage(PASSWORD_INVALID);
        message.sendToTarget();
    }

    /**
     * Is called once the whole login process is complete. That is, the login has been validated
     * by the server and all of the user's game data has been received and processed by the
     * LoginThread.
     *
     * Note that a loginSuccess() call will ALWAYS proceed a loginComplete() call. However, it is
     * possible for there to be a loginSuccess() call without a corresponding loginComplete() call
     * if the server encounters an error or the LoginThread encounters a SystemError after having
     * called loginSuccess().
     *
     * The Thread passes the result, a list of Game objects, each representing a game the logged-in
     * user is playing, to this method.
     */
    @Override
    public void loginComplete(List<Game> games) {
        this.notifyContainerRequestOver();

        Message message = this.obtainMessage(LOGIN_COMPLETE, games);
        message.sendToTarget();
    }

    /**
     * We use this method to communicate events pertaining to an active login request to the UI
     * thread. Note that this object only receives messages from itself.
     * @param msg - the received message
     */
    @Override
    public void handleMessage(@NonNull Message msg) {
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
            case LOGIN_SUCCESS:
                requester.loginSuccess();
                break;
            case USERNAME_INVALID:
                requester.usernameInvalid();
                break;
            case PASSWORD_INVALID:
                requester.passwordInvalid();
                break;
            case LOGIN_COMPLETE:
                requester.loginComplete((List<Game>)msg.obj);
                break;
        }
    }
}
