package com.lukaswillsie.onlinechess.activities;

import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

/**
 * A class that generalizes the process of setting up EditTexts in our application. The way we have
 * designed them, all EditTexts initially have alpha of 0.5, but change to 1 when selected by the
 * user. This class just establishes this behaviour here so that we don't have multiple boiler-plate
 * anonymous classes in a bunch of Activities' onCreate() methods.
 *
 */
public abstract class EditTextActivity extends AppCompatActivity {
    /**
     * Style the given EditText so that it becomes darker when clicked on, and more transparent when
     * moved away from.
     *
     * @param view - the EditText on which to place an OnFocusChangeListener
     */
    public void styleEditText(EditText view) {
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    v.setAlpha(1);
                }
                else {
                    v.setAlpha(0.5f);
                }
            }
        });
    }
}
