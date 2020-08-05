package com.lukaswillsie.onlinechess.activities;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.lukaswillsie.onlinechess.R;

/**
 * This class provides some static methods for customizing some UI elements, like Toasts.
 */
public class Display {
    /**
     * Creates and shows a Toast using the given Context, displaying the given message for the given
     * duration. Applies a custom style by changing the Toast's background colour and the colour of
     * its text. This method then calls .show() on the created Toast.
     * <p>
     * Calling this method is equivalent to calling Toast.makeText(context, msg, duration).show()
     * but with altered style.
     *
     * @param context  - the Context in which to create the Toast
     * @param msg      - the message to display in the Toast
     * @param duration - how long to display the Toast for. Should be either Toast.LENGTH_LONG or
     *                 Toast.LENGTH_SHORT
     */
    public static void makeToast(Context context, CharSequence msg, int duration) {
        Toast toast = Toast.makeText(context, msg, duration);
        View view = toast.getView();
        view.getBackground().setColorFilter(context.getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
        TextView text = view.findViewById(android.R.id.message);
        text.setTextColor(context.getResources().getColor(R.color.black));

        toast.show();
    }

    /**
     * Creates and shows a Toast using the given Context, displaying the given message for the given
     * duration. Applies a custom style by changing the Toast's background colour and the colour of
     * its text. This method then calls .show() on the created Toast.
     * <p>
     * Calling this method is equivalent to calling Toast.makeText(context, msg, duration).show()
     * but with altered style.
     *
     * @param context  - the Context in which to create the Toast
     * @param resId    - the id of the message to display in the Toast
     * @param duration - how long to display the Toast for. Should be either Toast.LENGTH_LONG or
     *                 Toast.LENGTH_SHORT
     */
    public static void makeToast(Context context, @StringRes int resId, int duration) {
        Toast toast = Toast.makeText(context, resId, duration);
        View view = toast.getView();
        view.getBackground().setColorFilter(context.getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
        TextView text = view.findViewById(android.R.id.message);
        text.setTextColor(context.getResources().getColor(R.color.black));

        toast.show();
    }

    /**
     * Creates a simple AlertDialog that just displays the given message, along with an OK button
     * that does nothing but disperse the dialog when clicked
     *
     * @param resId   - the ID of the string resource to display as a message in the dialog
     * @param context - the Context to create the dialog in
     */
    public static void showSimpleDialog(@StringRes int resId, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(resId).setPositiveButton(R.string.ok_dialog_button, null).show();
    }

    /**
     * Hide the keyboard for the given activity
     * <p>
     * This code was found on StackOverflow at the following address:
     * <p>
     * https://stackoverflow.com/questions/1109022/close-hide-android-soft-keyboard?answertab=votes#tab-top
     *
     * @param activity - the activity for which we are hiding the keyboard
     */
    public static void hideKeyboard(AppCompatActivity activity) {
        InputMethodManager manager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();

        if (view == null) {
            view = new View(activity);
        }

        manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
