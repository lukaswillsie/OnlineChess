package com.lukaswillsie.onlinechess.activities.load;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.lukaswillsie.onlinechess.ChessApplication;
import com.lukaswillsie.onlinechess.R;
import com.lukaswillsie.onlinechess.activities.ErrorDialogFragment;
import com.lukaswillsie.onlinechess.activities.login.LoginActivity;
import com.lukaswillsie.onlinechess.network.Connector;
import com.lukaswillsie.onlinechess.network.ServerHelper;
import com.lukaswillsie.onlinechess.network.threads.MultipleRequestException;

public class LoadActivity extends AppCompatActivity implements Connector, ErrorDialogFragment.ErrorDialogListener {
    /**
     * Tag for logging information to the console
     */
    private static final String tag = "LoadActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        // Immediately try and establish a connection with the server.
        try {
            new ServerHelper(this).connect(this);
        }
        catch (MultipleRequestException e) {
            Log.e(tag, "Multiple network requests occurred. Retrying connection");
            retry();
        }
    }

    /**
     * After a ServerHelper is tasked with establishing a connection, they will call this method on
     * the success, and pass a reference to themselves so they can be used for future network
     * operations.
     *
     * This implementation puts the ServerHelper as a Serializable extra into an Intent and then
     * starts LoginActivity
     *
     * @param helper - the ServerHelper object that has successfully established a connection with
     *               the server
     */
    @Override
    public void connectionEstablished(ServerHelper helper) {
        Intent intent = new Intent(this, LoginActivity.class);

        // We add the ServerHelper to ChessApplication for use by all subsequent activities
        ((ChessApplication)getApplicationContext()).setServerHelper(helper);
        this.startActivity(intent);
    }

    /**
     * If a ServerHelper has been tasked by an implementation of this interface to establish a
     * connection, and the connection could not be successfully created, the ServerHelper will
     * call this method so that this event can be handled.
     *
     * This implementation creates a dialog box to inform the user of the failure and present the
     * option to try again.
     */
    @Override
    public void connectionFailed() {
        DialogFragment failedDialog = new ErrorDialogFragment(this, getResources().getString(R.string.connection_failed_alert));
        failedDialog.show(getSupportFragmentManager(), "connection_failed_dialog");
    }

    public void retry() {
        try {
            new ServerHelper(this).connect(this);
        }
        catch (MultipleRequestException e) {
            Log.e(tag, "Multiple network requests occurred. Retrying connection");
            retry();
        }
    }

    @Override
    public void systemError() {
        // TODO: Implement this to be tailored more specifically to a network error
        this.connectionFailed();
    }
}
