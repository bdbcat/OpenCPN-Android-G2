package org.opencpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import org.opencpn.opencpn.R;

public class StorageLocationDialog {

    public enum StorageLocation {
        APP_PRIVATE,
        SHARED_DOCUMENTS,
        CANCELLED
    }

    public interface Callback {
        void onLocationSelected(StorageLocation location,
                                boolean rememberChoice);
    }

    public static void xshow(Activity activity,
                            boolean isImport,
                            Callback callback) {

        final StorageLocation[] selected =
                { StorageLocation.APP_PRIVATE };

        final boolean[] remember =
                { false };

        String title = "Choose Location";

        String message;

        if (isImport) {
            message =
                    "Where should the file be imported from?";
        } else {
            message =
                    "Where should the file be saved?";
        }

        String[] choices;

        if (isImport) {

            choices = new String[] {
                    "OpenCPN Storage\nEasy access from a connected PC",
                    "Device Documents\nChoose files shared by Android apps"
            };

        } else {

            choices = new String[] {
                    "OpenCPN Storage\nEasy access from a connected PC",
                    "Device Documents\nAccessible to other Android apps"
            };
        }

        AlertDialog.Builder builder =
                new AlertDialog.Builder(activity);

        builder.setTitle(title);

        builder.setMessage(message);

        builder.setSingleChoiceItems(
                choices,
                0,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {

                        if (which == 0) {
                            selected[0] =
                                    StorageLocation.APP_PRIVATE;
                        } else {
                            selected[0] =
                                    StorageLocation.SHARED_DOCUMENTS;
                        }
                    }
                });

        builder.setMultiChoiceItems(
                new String[] { "Remember my choice" },
                new boolean[] { false },
                new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which,
                                        boolean isChecked) {

                        remember[0] = isChecked;
                    }
                });

        builder.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {

                        callback.onLocationSelected(
                                StorageLocation.CANCELLED,
                                false);

                        dialog.dismiss();
                    }
                });

        builder.setPositiveButton(
                "Continue",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {

                        callback.onLocationSelected(
                                selected[0],
                                remember[0]);

                        dialog.dismiss();
                    }
                });

        builder.setCancelable(true);

        builder.show();
    }
    public static void show(Activity activity,
                             boolean isImport,
                             Callback callback) {
        Context themedContext =
                new ContextThemeWrapper(
                        activity,
                        R.style.OpenCPNThemeMaterial);

        LayoutInflater inflater =
                LayoutInflater.from(themedContext);

        View view =
                inflater.inflate(
                        R.layout.dialog_storage_location,
                        null);

        MaterialCardView cardPrivate =
                view.findViewById(R.id.card_private);

        MaterialCardView cardDocuments =
                view.findViewById(R.id.card_documents);

        CheckBox checkRemember =
                view.findViewById(R.id.check_remember);

        final StorageLocation[] selected =
                { StorageLocation.APP_PRIVATE };

        cardPrivate.setChecked(true);

        cardPrivate.setOnClickListener(v -> {

            selected[0] = StorageLocation.APP_PRIVATE;

            cardPrivate.setChecked(true);
            cardDocuments.setChecked(false);
        });

        cardDocuments.setOnClickListener(v -> {

            selected[0] = StorageLocation.SHARED_DOCUMENTS;

            cardPrivate.setChecked(false);
            cardDocuments.setChecked(true);
        });

        TextView subtitle =
                view.findViewById(R.id.subtitle);

        TextView documentsDescription =
                view.findViewById(R.id.documents_description);

        if (isImport) {
            subtitle.setText(
                    "Where should the file be imported from?");

            documentsDescription.setText(
                    "Choose files shared by Android apps");
        } else {
            subtitle.setText(
                    "Where should the file be saved?");

            documentsDescription.setText(
                    "Accessible to other Android apps");

        }

        AlertDialog dialog =
                new AlertDialog.Builder(themedContext)
                        .setView(view)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Continue",
                                (d, which) -> {
                                    boolean rememberChoice =
                                            checkRemember.isChecked();
                                    callback.onLocationSelected(
                                            selected[0],
                                            rememberChoice);
                                })
                        .create();

        dialog.show();
    }


}