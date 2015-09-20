package svenmeier.coxswain.view;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;
import android.widget.EditText;

import svenmeier.coxswain.R;


/**
 */
public class NameDialogFragment extends DialogFragment {

    private EditText editText;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        editText = new EditText(getActivity());

        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.action_rename)
                .setView(editText)
                .setPositiveButton(R.string.action_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Utils.getParent(NameDialogFragment.this, NameHolder.class).setName(editText.getText().toString());

                                dismiss();
                            }
                        }
                )
                .create();

        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return alertDialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        editText.setText(Utils.getParent(this, NameHolder.class).getName());
    }

    public static interface NameHolder {
        public String getName();

        public void setName(String name);
    }
}