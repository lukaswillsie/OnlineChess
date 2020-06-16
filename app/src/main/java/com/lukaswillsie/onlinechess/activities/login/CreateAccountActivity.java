package com.lukaswillsie.onlinechess.activities.login;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.EditTextActivity;
import com.lukaswillsie.onlinechess.network.helper.CreateAccountRequester;
import com.lukaswillsie.onlinechess.network.helper.ServerHelper;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

public class CreateAccountActivity extends EditTextActivity implements CreateAccountRequester {
    private static final String tag = "CreateAccountActivity";

    private ServerHelper serverHelper;
    @Override
    public void createAccountSuccess() {
        Log.i(tag, "ACCOUNT CREATED");
    }

    @Override
    public void usernameInUse() {
        Log.i(tag, "USERNAME IN USE");
    }

    @Override
    public void formatInvalid() {
        Log.i(tag, "FORMAT INVALID");
    }

    @Override
    public void connectionLost() {
        Log.i(tag, "CONNECTION LOST");
    }

    @Override
    public void serverError() {
        Log.i(tag, "SERVER ERROR");
    }

    @Override
    public void systemError() {
        Log.i(tag, "SYSTEM ERROR");
    }

    private enum State {
        WAITING_FOR_USER_INPUT,
        PROCESSING;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        EditText createUsername = findViewById(R.id.create_username);
        EditText createPassword = findViewById(R.id.create_password);
        EditText confirmPassword = findViewById(R.id.confirm_password);

        // Add OnFocusChangeListeners to all EditTexts so that they are transparent when not active,
        // but become darker when clicked on
        this.styleEditText(createUsername);
        this.styleEditText(createPassword);
        this.styleEditText(confirmPassword);

        this.serverHelper = ((ChessApplication)getApplicationContext()).getServerHelper();
    }

    /**
     * Called when the user clicks "Create Account". Grabs information from the EditTexts and sends
     * the request to the server. Also closes the keyboard, deactivates the EditTexts, and changes
     * the colour of the "Create Account" button to indicate that the request is being processed.
     * @param view
     */
    public void create(View view) {
        EditText createUsername = findViewById(R.id.create_username);
        EditText createPassword = findViewById(R.id.create_password);
        EditText confirmPassword = findViewById(R.id.confirm_password);

        createUsername.setFocusable(false);
        createPassword.setFocusable(false);
        confirmPassword.setFocusable(false);
        this.hideKeyboard();

        CardView card = findViewById(R.id.create_account_button);
        card.setCardBackgroundColor(getResources().getColor(R.color.loginLoading));

        TextView buttonText = findViewById(R.id.create_account_button_text);
        buttonText.setText(R.string.account_creation_processing_text);

        findViewById(R.id.create_account_progress).setVisibility(View.VISIBLE);

        String username = ((EditText)createUsername).getText().toString();
        String password = ((EditText)createPassword).getText().toString();
        String confirm = ((EditText)confirmPassword).getText().toString();

        try {
            serverHelper.createAccount(this, username, password);
        } catch (MultipleRequestException e) {
            Log.e(tag, "Submitted multiple requests to ServerHelper");
        }
    }

    /**
     * Hide the keyboard, so that we present the user with a nice clean loading interface after they
     * press the CREATE_ACCOUNT button.
     *
     * This code was found on StackOverflow at the following address:
     *
     * https://stackoverflow.com/questions/1109022/close-hide-android-soft-keyboard?answertab=votes#tab-top
     */
    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = getCurrentFocus();

        if(view == null) {
            view = new View(this);
        }

        manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
