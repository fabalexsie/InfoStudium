package de.siebes.fabian.infostudium;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MasterPassDialog extends AlertDialog.Builder {
    private OnSubmitListener mOnSubmitListener;
    private DialogInterface.OnCancelListener mOnCancelListener;
    private DialogInterface.OnClickListener mNegativeButtonClickListener;
    private int mNegativeButtonTextId = -1;
    private DialogInterface.OnClickListener mNeutralButtonClickListener;
    private int mNeutralButtonTextId = -1;

    /**
     * Creates a builder for an alert dialog that uses the default alert
     * dialog theme.
     * <p>
     * The default alert dialog theme is defined by
     * {@link android.R.attr#alertDialogTheme} within the parent
     * {@code context}'s theme.
     *
     * @param context the parent context
     */
    public MasterPassDialog(final Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view;

        view = inflater.inflate(R.layout.dialog_check_master_pass, null);


        setView(view);

        setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mOnSubmitListener != null) {
                    String strMasterpass = ((EditText) view.findViewById(R.id.etMasterpass)).getText().toString();
                    if (!mOnSubmitListener.onSubmit(strMasterpass)) {
                        Toast.makeText(context, R.string.masterpass_wrong, Toast.LENGTH_LONG).show();
                        AlertDialog.Builder builder = new MasterPassDialog(context)
                                .setOnSubmitListener(mOnSubmitListener)
                                .setOnCancelListener(mOnCancelListener);
                        if (mNegativeButtonTextId >= 0)
                            builder.setNegativeButton(mNegativeButtonTextId, mNegativeButtonClickListener);
                        if (mNeutralButtonTextId >= 0)
                            builder.setNeutralButton(mNeutralButtonTextId, mNeutralButtonClickListener);
                        builder.show();
                    }
                }
            }
        });
        setNegativeButton(android.R.string.cancel, null);
    }

    public MasterPassDialog setOnSubmitListener(OnSubmitListener onSubmitListener) {
        mOnSubmitListener = onSubmitListener;
        return this;
    }

    @Override
    public AlertDialog.Builder setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
        mOnCancelListener = onCancelListener;
        return super.setOnCancelListener(onCancelListener);
    }

    @Override
    public AlertDialog.Builder setNegativeButton(int textId, DialogInterface.OnClickListener listener) {
        mNegativeButtonTextId = textId;
        mNegativeButtonClickListener = listener;
        return super.setNegativeButton(textId, listener);
    }

    @Override
    public AlertDialog.Builder setNeutralButton(int textId, DialogInterface.OnClickListener listener) {
        mNeutralButtonTextId = textId;
        mNeutralButtonClickListener = listener;
        return super.setNeutralButton(textId, listener);
    }

    public interface OnSubmitListener {

        /**
         * @param strPass the password to check
         * @return false, if password isn't correct and the dialog should be reshown
         */
        boolean onSubmit(String strPass);
    }
}
