/*
 * Copyright (C) 2016 The CyanogenMod Project
 *           (C) 2017-2018 The LineageOS Project
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

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.MenuItem;
import android.util.Log;

import org.lineageos.internal.util.FileUtils;
import org.lineageos.internal.util.PackageManagerUtils;

public class ButtonSettingsFragment extends PreferenceFragment
        implements OnPreferenceChangeListener {

    private NotificationBrightnessPreference mNotificationBrightness;
    private TorchYellowBrightnessPreference mYellowBrightness;
    private TorchWhiteBrightnessPreference mWhiteBrightness;
    private VibratorStrengthPreference mVibratorStrength;
    private Preference mKcalPref;
    private ListPreference mGOVERNOR;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.button_panel);
        final ActionBar actionBar = getActivity().getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mGOVERNOR = (ListPreference) findPreference(Constants.GOVERNOR_KEY);
        mGOVERNOR.setValue(FileUtils.getStringProp(Constants.GOVERNOR_SYSTEM_PROPERTY, "0"));
        mGOVERNOR.setOnPreferenceChangeListener(this);

        mNotificationBrightness = (NotificationBrightnessPreference) findPreference("notification_key");
        if (mNotificationBrightness != null) {
            mNotificationBrightness.setEnabled(NotificationBrightnessPreference.isSupported());
        }

        mYellowBrightness = (TorchYellowBrightnessPreference) findPreference("torch_yellow_key");
        if (mYellowBrightness != null) {
            mYellowBrightness.setEnabled(TorchYellowBrightnessPreference.isSupported());
        }

        mWhiteBrightness = (TorchWhiteBrightnessPreference) findPreference("torch_white_key");
        if (mWhiteBrightness != null) {
            mWhiteBrightness.setEnabled(TorchWhiteBrightnessPreference.isSupported());
        }

        mVibratorStrength = (VibratorStrengthPreference) findPreference("vibrator_key");
        if (mVibratorStrength != null) {
            mVibratorStrength.setEnabled(VibratorStrengthPreference.isSupported());
        }

        mKcalPref = findPreference("kcal");
        mKcalPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
             @Override
             public boolean onPreferenceClick(Preference preference) {
                 Intent intent = new Intent(getActivity(), DisplayCalibration.class);
                 startActivity(intent);
                 return true;
             }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferencesBasedOnDependencies();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String node = Constants.sBooleanNodePreferenceMap.get(preference.getKey());
        final String key = preference.getKey();
        if (!TextUtils.isEmpty(node) && FileUtils.isFileWritable(node)) {
            Boolean value = (Boolean) newValue;
            FileUtils.writeLine(node, value ? "1" : "0");
            return true;
        }
        node = Constants.sStringNodePreferenceMap.get(preference.getKey());
        if (!TextUtils.isEmpty(node) && FileUtils.isFileWritable(node)) {
            FileUtils.writeLine(node, (String) newValue);
            return true;
        }
	if (Constants.GOVERNOR_KEY.equals(key)) {
            Log.d("ConfigPanel",  "onPreferenceChange: " + newValue.toString());
            mGOVERNOR.setValue((String) newValue);
            FileUtils.setStringProp(Constants.GOVERNOR_SYSTEM_PROPERTY, (String) newValue);
	    return true;
	}
        return false;
    }

    @Override
    public void addPreferencesFromResource(int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        // Initialize node preferences
        for (String pref : Constants.sBooleanNodePreferenceMap.keySet()) {
            SwitchPreference b = (SwitchPreference) findPreference(pref);
            if (b == null) continue;
            b.setOnPreferenceChangeListener(this);
            String node = Constants.sBooleanNodePreferenceMap.get(pref);
            if (FileUtils.isFileReadable(node)) {
                String curNodeValue = FileUtils.readOneLine(node);
                b.setChecked(curNodeValue.equals("1"));
            } else {
                b.setEnabled(false);
            }
        }
        for (String pref : Constants.sStringNodePreferenceMap.keySet()) {
            ListPreference l = (ListPreference) findPreference(pref);
            if (l == null) continue;
            l.setOnPreferenceChangeListener(this);
            String node = Constants.sStringNodePreferenceMap.get(pref);
            if (FileUtils.isFileReadable(node)) {
                l.setValue(FileUtils.readOneLine(node));
            } else {
                l.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }

    private void updatePreferencesBasedOnDependencies() {
        for (String pref : Constants.sNodeDependencyMap.keySet()) {
            SwitchPreference b = (SwitchPreference) findPreference(pref);
            if (b == null) continue;
            String dependencyNode = Constants.sNodeDependencyMap.get(pref)[0];
            if (FileUtils.isFileReadable(dependencyNode)) {
                String dependencyNodeValue = FileUtils.readOneLine(dependencyNode);
                boolean shouldSetEnabled = dependencyNodeValue.equals(
                        Constants.sNodeDependencyMap.get(pref)[1]);
                Utils.updateDependentPreference(getContext(), b, pref, shouldSetEnabled);
            }
        }
    }
    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof VibratorStrengthPreference){
            ((VibratorStrengthPreference)preference).onDisplayPreferenceDialog(preference);
        } else if (preference instanceof NotificationBrightnessPreference){
            ((NotificationBrightnessPreference)preference).onDisplayPreferenceDialog(preference);
        } else if (preference instanceof TorchYellowBrightnessPreference){
            ((TorchYellowBrightnessPreference)preference).onDisplayPreferenceDialog(preference);
        } else if (preference instanceof TorchWhiteBrightnessPreference){
            ((TorchWhiteBrightnessPreference)preference).onDisplayPreferenceDialog(preference);
	} else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
