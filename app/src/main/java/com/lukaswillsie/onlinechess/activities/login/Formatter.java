package com.lukaswillsie.onlinechess.activities.login;

import android.view.View;
import android.widget.EditText;

public class Formatter {
    /**
     * Style the given EditText so that it becomes darker when clicked on, and more transparent when
     * moved away from.
     *
     * @param view - the EditText on which to place an OnFocusChangeListener
     */
    public static void styleEditText(EditText view) {
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.setAlpha(1);
                } else {
                    v.setAlpha(0.5f);
                }
            }
        });
    }
}
