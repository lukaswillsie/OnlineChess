package com.lukaswillsie.onlinechess.activities;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.lukaswillsie.onlinechess.R;

/**
 * This class provides some static methods for customizing some UI elements, like Toasts.
 */
public class Display {
    /**
     * Creates and shows a Toast using the given Context, displaying the given message for the given
     * duration. Applies a custom style by changing the Toast's background colour and the colour of
     * its text. This method then calls .show() on the created Toast.
     *
     * Calling this method is equivalent to calling Toast.makeText(context, msg, duration).show()
     * but with altered style.
     *
     * @param context - the Context in which to create the Toast
     * @param msg - the message to display in the Toast
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
     *
     * Calling this method is equivalent to calling Toast.makeText(context, msg, duration).show()
     * but with altered style.
     *
     * @param context - the Context in which to create the Toast
     * @param resId - the id of the message to display in the Toast
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
}
