/*
* Copyright (C) 2016 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.lineageos.settings.device;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.database.ContentObserver;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.Preference;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.os.Bundle;
import android.util.Log;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.app.AlertDialog;

import java.util.List;

import org.lineageos.settings.device.Utils;
import org.lineageos.settings.device.R;

public class VibratorStrengthPreference extends DialogPreference implements
        SeekBar.OnSeekBarChangeListener {

    public static final String KEY_VIBSTRENGTH = "vib_strength";

    private SeekBar mSeekBar;
    private int mOldStrength;
    private int mMinValue;
    private int mMaxValue;
    private float offset;
    private Vibrator mVibrator;
    private TextView mValueText;

    private static final String FILE_LEVEL = "/sys/class/timed_output/vibrator/vtg_level";
    private static final String FILE_MIN = "/sys/class/timed_output/vibrator/vtg_min";
    private static final String FILE_MAX = "/sys/class/timed_output/vibrator/vtg_max";
    private static final long testVibrationPattern[] = {0,250};

    public VibratorStrengthPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);                
    }

    public boolean onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof VibratorStrengthPreference) {
        	mOldStrength = Integer.parseInt(getValue(getContext()));
			mMinValue = Integer.parseInt(Utils.getFileValue(FILE_MIN, "0"));
        	mMaxValue = Integer.parseInt(Utils.getFileValue(FILE_MAX, "100"));
        	offset = mMaxValue/100f;

            View view = LayoutInflater.from(getContext()).inflate(R.layout.preference_dialog_vibrator_strength, null);            
            mSeekBar = (SeekBar)view.findViewById(R.id.seekbar);
            mValueText = (TextView) view.findViewById(R.id.current_value);

            mSeekBar.setMax(mMaxValue - mMinValue);
            mSeekBar.setProgress(mOldStrength - mMinValue);
            mValueText.setText(Integer.toString(Math.round(mOldStrength / offset)) + "%");            
            mSeekBar.setOnSeekBarChangeListener(this);

            new AlertDialog.Builder(getContext())
                    .setView(view)
                    .setTitle(getContext().getString(R.string.vibrator_summary))
                    .setPositiveButton(android.R.string.ok, clickPositiveButton())
                    .setNegativeButton(android.R.string.cancel, clickNegativeButton())
                    .show();

            return true;
        }
        return false;
    }

    private DialogInterface.OnClickListener clickNegativeButton() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restoreOldState();

                mVibrator.cancel();
            }
        };
    }

    private DialogInterface.OnClickListener clickPositiveButton() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final int value = mSeekBar.getProgress() + mMinValue;
                setValue(String.valueOf(value));
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                editor.putString(KEY_VIBSTRENGTH, String.valueOf(value));
                editor.commit();

                mVibrator.cancel();
            }
        };
    }

    public static boolean isSupported() {
        return Utils.fileWritable(FILE_LEVEL);
    }

    public static String getValue(Context context) {
        return Utils.getFileValue(FILE_LEVEL, "100");
    }

    private void setValue(String newValue) {
        Utils.writeValue(FILE_LEVEL, newValue);
    }

    public static void restore(Context context) {
        if (!isSupported()) {
            return;
        }

        String storedValue = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_VIBSTRENGTH, "100"); 
        Utils.writeValue(FILE_LEVEL, storedValue);
    }

    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromTouch) {
        setValue(String.valueOf(progress + mMinValue));
        mValueText.setText(Integer.toString(Math.round((progress + mMinValue) / offset)) + "%");        
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        // NA
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mVibrator.hasVibrator())
            mVibrator.vibrate(testVibrationPattern, -1);
    }

    private void restoreOldState() {
        setValue(String.valueOf(mOldStrength));
    }

    public void createActionButtons() {
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }
}
