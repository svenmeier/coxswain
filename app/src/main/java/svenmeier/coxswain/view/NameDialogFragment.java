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
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.EditText;

import propoid.db.Reference;
import propoid.ui.bind.TextBinding;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Program;


/**
 */
public class NameDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Program program = Gym.instance(getActivity()).getProgram(Reference.<Program>from(getArguments()));

        final EditText editText = new EditText(getActivity());

        TextBinding.string(program.name, editText);

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.action_rename)
                .setView(editText)
                .setPositiveButton(R.string.action_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Gym.instance(getActivity()).mergeProgram(program);

                                dismiss();
                            }
                        }
                )
                .create();

        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return alertDialog;
    }

    public static NameDialogFragment create(Program program) {
        NameDialogFragment fragment = new NameDialogFragment();

        fragment.setArguments(new Reference<>(program).to(new Bundle()));

        return fragment;
    }
}