/*
 * Copyright 2015 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package svenmeier.coxswain.view;

import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.Collection;

import svenmeier.coxswain.GymService;
import svenmeier.coxswain.R;
import svenmeier.coxswain.rower.wired.usb.UsbConnector;

public class DevicesFragment extends PreferenceFragmentCompat {

    private UsbConnector connector;

    @Override
    public void onStart() {
        super.onStart();

        connector = new UsbConnector(getContext()) {
            @Override
            protected void onConnected(UsbDevice device) {
                GymService.start(getContext(), device);
            }
        };
    }

    @Override
    public void onStop() {
        super.onStop();

        if (connector != null) {
            connector.destroy();
            connector = null;
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(getActivity());

        Collection<UsbDevice> devices = new UsbConnector(getActivity()).list();
        if (devices.isEmpty()) {
            Preference preference = new Preference(getActivity());

            preference.setTitle(getString(R.string.preference_devices_none_title));
            preference.setSummary(getString(R.string.preference_devices_none_summary));

            screen.addPreference(preference);
        } else {
            for (final UsbDevice device : devices) {
                Preference preference = new Preference(getActivity());

                preference.setTitle(getTitle(device));

                StringBuilder summary = new StringBuilder();
                append(summary, "Vendor", device.getVendorId());
                append(summary, "Product", device.getProductId());
                append(summary, "Class", device.getDeviceClass());

                preference.setSummary(summary);

                preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        connector.connect(device);

                        return true;
                    }
                });

                screen.addPreference(preference);
            }
        }

        setPreferenceScreen(screen);
    }

    private String getTitle(UsbDevice device) {
        String title = device.getDeviceName();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            title = device.getProductName();
        }

        return title;
    }

    private void append(StringBuilder summary, String key, Object value) {
        if (summary.length() > 0) {
            summary.append("\n");
        }
        summary.append(key);
        summary.append(": " );
        summary.append(value);
    }
}