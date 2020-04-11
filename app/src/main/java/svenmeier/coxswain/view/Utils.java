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

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.TypedValue;

import androidx.fragment.app.Fragment;

/**
 * Created by sven on 19.08.15.
 */
public class Utils {

    public static float dpToPx(Context context, int dp) {

        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    @SuppressWarnings("unchecked")
    public static <T> T getCallback(Fragment fragment, Class<T> callback) {
        while (true) {
            if (callback.isInstance(fragment)) {
                return (T) fragment;
            }

            Fragment parentFragment = fragment.getParentFragment();
            if (parentFragment == null) {
                break;
            }
            fragment = parentFragment;
        }

        return getCallback(fragment.getActivity(), callback);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getCallback(Activity activity, Class<T> callback) {
        if (activity != null && callback.isInstance(activity)) {
            return (T) activity;
        }

        Application application = activity.getApplication();
        if (application != null && callback.isInstance(application)) {
            return (T) application;
        }

        throw new IllegalStateException("no requested parental callback " + callback.getSimpleName());
    }
}
