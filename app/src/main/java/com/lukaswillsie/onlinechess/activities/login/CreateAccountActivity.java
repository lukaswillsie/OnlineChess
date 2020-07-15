package com.lukaswillsie.onlinechess.activities.login;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.ErrorDialogActivity;
import com.lukaswillsie.onlinechess.activities.MainActivity;
import com.lukaswillsie.onlinechess.data.UserGame;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;
import com.lukaswillsie.onlinechess.network.helper.requesters.CreateAccountRequester;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

import java.util.ArrayList;

/**
 * Activity class representing the screen that users are taken to when they want to create a new
 * account.
 */
public class CreateAccountActivity extends ErrorDialogActivity implements CreateAccountRequester {
    /**
     * Tag used for logging to the console
     */
    private static final String tag = "CreateAccountActivity";

    /*
     * The ServerHelper this activity will use to communicate with the server
     */
    private ServerHelper serverHelper;

    /*
     * Represents the activity's current state
     */
    private State state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        EditText createUsername = findViewById(R.id.create_username);
        EditText createPassword = findViewById(R.id.create_password);
        EditText confirmPassword = findViewById(R.id.confirm_password);

        // Add OnFocusChangeListeners to all EditTexts so that they are transparent when not active,
        // but become darker when clicked on
        Formatter.styleEditText(createUsername);
        Formatter.styleEditText(createPassword);
        Formatter.styleEditText(confirmPassword);

        this.serverHelper = ((ChessApplication) getApplicationContext()).getServerHelper();
        this.state = State.WAITING_FOR_USER_INPUT;
    }

    /**
     * Called when the server responds that an account creation request was successful
     */
    @Override
    public void createAccountSuccess() {
        if (this.state == State.PROCESSING) {
            // Save an empty list of games (because we've created a totally new user), as well as
            // the user's username and password, globally in ChessApplication
            ChessApplication application = (ChessApplication) getApplicationContext();
            application.setGames(new ArrayList<UserGame>());
            application.login(((EditText)findViewById(R.id.create_username)).getText().toString());

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Called when the server responds that the given username is already in use
     */
    @Override
    public void usernameInUse() {
        if (this.state == State.PROCESSING) {
            // Display an error message
            TextView errorText = findViewById(R.id.create_account_input_error);
            errorText.setText(R.string.username_in_use_error);
            errorText.setVisibility(View.VISIBLE);

            resetUI();
            this.state = State.WAITING_FOR_USER_INPUT;
        }
    }

    /**
     * Called when the server responds that the given username and/or password are invalidly
     * formatted and can't be accepted
     */
    @Override
    public void formatInvalid() {
        if (this.state == State.PROCESSING) {
            // Display an error message
            TextView errorText = findViewById(R.id.create_account_input_error);
            errorText.setText(R.string.format_invalid_error);
            errorText.setVisibility(View.VISIBLE);

            resetUI();

            this.state = State.WAITING_FOR_USER_INPUT;
        }
    }

    /**
     * To be called if a request has been made (other than simply a connect request) but the server
     * is found to be unresponsive during the course of handling the request
     */
    @Override
    public void connectionLost() {
        this.createConnectionLostDialog();
    }

    /**
     * To be called if the server ever responds to a request with ReturnCodes.SERVER_ERROR, or if
     * the data received from the server is wrong, and doesn't correspond to its established
     * protocols. Basically, if something inexplicable happened server-side.
     */
    @Override
    public void serverError() {
        this.createServerErrorDialog();
    }

    /**
     * This method will be called if a system error occurs during the processing of a network
     * request. For example, if the internet has gone down, or an output/input stream cannot be
     * opened, or is causing some other problem. Simply allows the calling activity to differentiate
     * how it communicates errors to the user.
     */
    @Override
    public void systemError() {
        this.createSystemErrorDialog();
    }

    /**
     * Provides behaviour in the event that the user presses "Try Again" on a system error dialog
     */
    @Override
    public void retrySystemError() {
        this.processCreateAccount();
    }

    /**
     * Provides behaviour in the event that the user presses "Cancel" on a system error dialog
     */
    @Override
    public void cancelSystemError() {
        this.resetUI();
        this.state = State.WAITING_FOR_USER_INPUT;
    }

    /**
     * Provides behaviour in the event that the user presses "Try Again" on a server error dialog
     */
    @Override
    public void retryServerError() {
        this.state = State.WAITING_FOR_USER_INPUT;
        this.processCreateAccount();
    }

    /**
     * Provides behaviour in the event that the user presses "Cancel" on a server error dialog
     */
    @Override
    public void cancelServerError() {
        this.resetUI();
        this.state = State.WAITING_FOR_USER_INPUT;
    }

    /**
     * Provides behaviour in the event that the user presses "Try Again" on a lost connection error
     * dialog.
     */
    @Override
    public void retryConnection() {
        // TODO: Decide what to do here.
    }

    /**
     * Called when the user clicks "Create Account". Grabs information from the EditTexts and sends
     * the request to the server. Also closes the keyboard, deactivates the EditTexts, and changes
     * the colour of the "Create Account" button to indicate that the request is being processed.
     *
     * @param view - the view that was clicked
     */
    public void create(View view) {
        processCreateAccount();
    }

    /**
     * Process an account creation request using the data that is currently entered into the
     * on-screen EditTexts
     */
    public void processCreateAccount() {
        if (this.state == State.WAITING_FOR_USER_INPUT) {
            // Hide any error text that may be showing
            findViewById(R.id.create_account_input_error).setVisibility(View.INVISIBLE);

            // Get a reference to each of the EditTexts on the screen
            EditText createUsername = findViewById(R.id.create_username);
            EditText createPassword = findViewById(R.id.create_password);
            EditText confirmPassword = findViewById(R.id.confirm_password);

            // Send the server a login request
            String username = createUsername.getText().toString();
            String password = createPassword.getText().toString();
            String confirm = confirmPassword.getText().toString();

            if (Format.validPassword(password) && Format.validUsername(username)) {
                if (password.equals(confirm)) {
                    // Disable the EditTexts and hide the keyboard
                    createUsername.setFocusable(false);
                    createPassword.setFocusable(false);
                    confirmPassword.setFocusable(false);
                    this.hideKeyboard();

                    // Change the color of the login button
                    CardView card = findViewById(R.id.create_account_button);
                    card.setCardBackgroundColor(getResources().getColor(R.color.loginLoading));

                    // Change the text of the login button
                    TextView buttonText = findViewById(R.id.create_account_button_text);
                    buttonText.setText(R.string.account_creation_processing_text);

                    // Reveal the progress bar in the login button
                    findViewById(R.id.create_account_progress).setVisibility(View.VISIBLE);

                    try {
                        serverHelper.createAccount(this, username, password);
                        this.state = State.PROCESSING;
                    } catch (MultipleRequestException e) {
                        Log.e(tag, "Submitted multiple requests to ServerHelper");
                    }
                } else {
                    // Display an error message
                    TextView errorText = findViewById(R.id.create_account_input_error);
                    errorText.setText(R.string.confirm_not_equal_to_password);
                    errorText.setVisibility(View.VISIBLE);
                }
            } else {
                // Display an error message
                TextView errorText = findViewById(R.id.create_account_input_error);
                errorText.setText(R.string.format_invalid_error);
                errorText.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Hide the keyboard, so that we present the user with a nice clean loading interface after they
     * press the CREATE ACCOUNT button.
     * <p>
     * This code was found on StackOverflow at the following address:
     * <p>
     * https://stackoverflow.com/questions/1109022/close-hide-android-soft-keyboard?answertab=votes#tab-top
     */
    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();

        if (view == null) {
            view = new View(this);
        }

        manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Revert the UI to its initial state. That is, reactivates the EditTexts, changes the colour
     * of the button to black, sets its text as "Create Account", and hides the ProgressBar
     */
    private void resetUI() {
        // Reactivate all three EditTexts
        EditText username = findViewById(R.id.create_username);
        username.setText("");
        username.setFocusableInTouchMode(true);
        username.setFocusable(true);

        EditText password = findViewById(R.id.create_password);
        password.setText("");
        password.setFocusableInTouchMode(true);
        password.setFocusable(true);

        EditText confirmPassword = findViewById(R.id.confirm_password);
        confirmPassword.setText("");
        confirmPassword.setFocusableInTouchMode(true);
        confirmPassword.setFocusable(true);

        // Revert the button back to black
        ((CardView) findViewById(R.id.create_account_button)).setCardBackgroundColor(Color.parseColor("#000000"));

        // Hide the ProgressBar
        findViewById(R.id.create_account_progress).setVisibility(View.INVISIBLE);

        // Reset the button text to "Create Account"
        ((TextView) findViewById(R.id.create_account_button_text)).setText(R.string.create_account_button);
    }

    /**
     * Represents the activity's current state; we can either be waiting for the user to press the
     * "Create Account" button or in the middle of processing an account creation request
     */
    private enum State {
        WAITING_FOR_USER_INPUT,
        PROCESSING
    }
}
