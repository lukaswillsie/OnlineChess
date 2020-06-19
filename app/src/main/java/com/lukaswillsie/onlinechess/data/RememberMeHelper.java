package com.lukaswillsie.onlinechess.data;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * This app provides a "Remember Me" automatic login feature by keeping a file in internal storage
 * that records a username and password saved using the "Remember Me" feature, as well as a
 * timestamp for allowing saved data to expire after a set amount of time.
 *
 * To be precise, the file has this simple format:
 * Timestamp
 * Username
 * Password
 *
 * and is located directly in this app's root directory on the device (the directory returned by
 * getFilesDir()).
 *
 * This data is erased and forgotten about once DAYS_TO_ELAPSE days have passed.
 */
public class RememberMeHelper {
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";
    public static final String ERROR_KEY = "error";

    private static final String TIMESTAMP_PATTERN = "uuuu-MM-dd";

    private static final String tag = "RememberMeHelper";
    private static final String SAVED_USER_DATA_FILE = "saved_user";
    private static final int DAYS_TO_ELAPSE = 5;
    private File savedUserFile;

    /**
     * Create a new RememberMeHelper from the given context. This constructor checks if the file to
     * be used for saving user data exists, and creates it if it doesn't.
     *
     * @param context - the context creating this RememberMeHelper (used to gain access to the
     *                appropriate folder)
     * @throws IOException - if the file to be used for saving user data cannot be created
     */
    public RememberMeHelper(Context context) throws IOException {
        this.savedUserFile = new File(context.getFilesDir(), SAVED_USER_DATA_FILE);

        if(savedUserFile.createNewFile()) {
            Log.i(tag, "File for saving user data created");
        }
        else {
            Log.i(tag, "File for saving user data already exists");
        }
    }

    /**
     * Return a HashMap with three keys (always): USERNAME_KEY, PASSWORD_KEY, and ERROR_KEY (see
     * above constants). If a system error occurs while accessing data, "error" is set to "1" (THE
     * STRING), and username and password are null. Otherwise, "error" is "0" (THE STRING), and
     * "username" and "password" are either set to the username and password of the saved user, if
     * there is one, or each null if there isn't.
     *
     *
     * @return A HashMap containing information about a saved user
     */
    public HashMap<String, String> savedUserData() {
        HashMap<String, String> map = new HashMap<>();

        Scanner scanner;
        try {
            scanner = new Scanner(savedUserFile);
        } catch (FileNotFoundException e) {
            Log.e(tag, "Couldn't open saved user data file for scanning");
            e.printStackTrace();
            map.put(USERNAME_KEY, null);
            map.put(PASSWORD_KEY, null);
            map.put(ERROR_KEY, "1");

            return map;
        }

        // If the file is empty, we have no saved user
        if(!scanner.hasNextLine()) {
            map.put(USERNAME_KEY, null);
            map.put(PASSWORD_KEY, null);
            map.put(ERROR_KEY, "0");

            return map;
        }

        String line = scanner.nextLine();
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP_PATTERN);

        Date date;
        try {
            date = dateFormat.parse(line);
        } catch (ParseException e) {
            Log.e(tag, "Couldn't parse line \"" + line + "\" from saved user data file into date");

            map.put(USERNAME_KEY, null);
            map.put(PASSWORD_KEY, null);
            map.put(ERROR_KEY, "1");
            return map;
        }

        Date now = new Date();
        // If the user's data was saved more than 5 days ago, we erase it and return no data
        if(TimeUnit.DAYS.convert(date.getTime() - now.getTime(), TimeUnit.MILLISECONDS) > 5) {
            try {
                // Opening the file with a FileOutputStream erases it
                FileOutputStream stream = new FileOutputStream(this.savedUserFile);
            } catch (FileNotFoundException e) {
                Log.e(tag, "Couldn't open saved user data file for erasing");

                map.put(USERNAME_KEY, null);
                map.put(PASSWORD_KEY, null);
                map.put(ERROR_KEY, "1");
                return map;
            }

            map.put(USERNAME_KEY, null);
            map.put(PASSWORD_KEY, null);
            map.put(ERROR_KEY, "0");
            return map;
        }
        else {
            if(!scanner.hasNextLine()) {
                Log.e(tag, "Not enough lines in saved user data file");

                map.put(USERNAME_KEY, null);
                map.put(PASSWORD_KEY, null);
                map.put(ERROR_KEY, "1");
                return map;
            }
            String username = scanner.nextLine();

            if(!scanner.hasNextLine()) {
                Log.e(tag, "Not enough lines in saved user data file");

                map.put(USERNAME_KEY, null);
                map.put(PASSWORD_KEY, null);
                map.put(ERROR_KEY, "1");
                return map;
            }
            String password = scanner.nextLine();

            map.put(USERNAME_KEY, username);
            map.put(PASSWORD_KEY, password);
            map.put(ERROR_KEY, "0");
            return map;
        }
    }

    /**
     * Takes the given username and password and saves them, so that the user will be automatically
     * logged in the next time they launch the app.
     *
     * Returns an integer detailing the success of the operation
     *
     * @param username - the username to save
     * @param password - the password to save
     * @return 0 if data is successfully saved, 1 if an error occurs
     */
    public int saveUser(String username, String password) {
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(this.savedUserFile);
        }
        catch(FileNotFoundException e) {
            Log.e(tag, "Couldn't open saved user data file for saving");
            return 1;
        }

        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat(TIMESTAMP_PATTERN);

        try {
            stream.write(format.format(now).getBytes());
            stream.write(username.getBytes());
            stream.write(password.getBytes());
        }
        catch(IOException e) {
            Log.e(tag, "Couldn't save user data: (" + username + "," + password + ")");
            return 1;
        }

        Log.i(tag, "User data: (" + username + "," + password + ") successfully saved");
        return 0;
    }
}
