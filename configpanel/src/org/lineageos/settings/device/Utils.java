/*
 * Copyright (C) 2016 The CyanogenMod Project
 *           (C) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.device;

import android.content.Context;
import android.content.Intent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceManager;
import java.io.BufferedReader;
import java.io.FileReader;

public class Utils {

    /**
     * Write a string value to the specified file.
     * @param filename      The filename
     * @param value         The value
     */
    public static void writeValue(String filename, String value) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(filename));
            fos.write(value.getBytes());
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean fileExists(String filename) {
        return new File(filename).exists();
    }

    public static boolean fileWritable(String filename) {
        return fileExists(filename) && new File(filename).canWrite();
    }

    public static boolean isPreferenceEnabled(Context context, String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(key, (Boolean) Constants.sNodeDefaultMap.get(key));
    }

    public static String getPreferenceString(Context context, String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(key, (String) Constants.sNodeDefaultMap.get(key));
    }

    public static void updateDependentPreference(Context context, SwitchPreference b,
            String key, boolean shouldSetEnabled) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean prefActualValue = preferences.getBoolean(key, false);

        if (shouldSetEnabled) {
            if (Constants.sNodeUserSetValuesMap.get(key) != null &&
                    (Boolean) Constants.sNodeUserSetValuesMap.get(key)[1] &&
                    (Boolean) Constants.sNodeUserSetValuesMap.get(key)[1] != prefActualValue) {
                b.setChecked(true);
                Constants.sNodeUserSetValuesMap.put(key, new Boolean[]{ prefActualValue, false });
            }
        } else {
            if (b.isEnabled() && prefActualValue) {
                Constants.sNodeUserSetValuesMap.put(key, new Boolean[]{ prefActualValue, true });
            }
            b.setEnabled(false);
            b.setChecked(false);
        }
    }

    public static void broadcastCustIntent(Context context, boolean value) {
        final Intent intent = new Intent(Constants.CUST_INTENT);
        context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    public static String getFileValue(String filename, String defValue) {
        String fileValue = readLine(filename);
        if(fileValue!=null){
            return fileValue;
        }
        return defValue;
    }
     public static String readLine(String filename) {
        if (filename == null) {
            return null;
        }
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(filename), 1024);
            line = br.readLine();
        } catch (IOException e) {
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return line;
    }
}
