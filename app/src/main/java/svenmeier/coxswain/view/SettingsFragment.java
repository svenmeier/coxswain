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

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.util.ArrayList;

import svenmeier.coxswain.R;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        Preference bindings = findPreference(getString(R.string.preference_workout_bindings_reset));
        bindings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                propoid.util.content.Preference.getEnum(getActivity(), ValueBinding.class, R.string.preference_workout_binding).setList(new ArrayList<ValueBinding>());
                propoid.util.content.Preference.getEnum(getActivity(), ValueBinding.class, R.string.preference_workout_binding_pace).setList(new ArrayList<ValueBinding>());

                return true;
            }
        });

        Preference devices = findPreference(getString(R.string.preference_devices));
        devices.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(android.R.id.content, new DevicesFragment());
                transaction.addToBackStack(null);
                transaction.commit();

                return true;
            }
        });
    }
}
