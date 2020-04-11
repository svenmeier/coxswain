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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import propoid.core.Propoid;
import propoid.db.Reference;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;

/**
 */
public class DeleteDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.action_delete_confirm_title)
                .setMessage(R.string.action_delete_confirm_message).setPositiveButton(R.string.action_delete_confirm, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        Propoid propoid = Gym.instance(getActivity()).get(Reference.from(getArguments()));
        Gym.instance(getActivity()).delete(propoid);

        dismiss();
    }

    public static DeleteDialogFragment create(Propoid propoid) {
        DeleteDialogFragment fragment = new DeleteDialogFragment();

        fragment.setArguments(new Reference<>(propoid).to(new Bundle()));

        return fragment;
    }
}