package org.opencpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.TypedValue;
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
    static final int COLOR_NORMAL = Color.LTGRAY;
    static final int COLOR_SELECTED = Color.rgb(40, 100, 200);
    static final int selectedStroke = Color.rgb(40, 100, 200);
    private static final int normalStroke = Color.rgb(40, 40, 40);;

    static final int dimTitle = Color.rgb(32, 32, 32);
    static final int dimDescription = Color.rgb(64, 64, 64);

    static final int selectedTitleColor  = Color.rgb(140, 140, 140);
    static final int selectedDescriptionColor = Color.rgb(170, 170, 170);


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

    private static int themeColor(Context context, int attr) {
        TypedValue tv = new TypedValue();

        if (!context.getTheme().resolveAttribute(attr, tv, true))
            return Color.TRANSPARENT;

        if (tv.resourceId != 0)
            return context.getResources().getColor(tv.resourceId, context.getTheme());

        return tv.data;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(
                alpha,
                Color.red(color),
                Color.green(color),
                Color.blue(color));
    }

    private static void selectCard(MaterialCardView selectedCard,
                            TextView selectedTitle,
                            TextView selectedDescription,
                            MaterialCardView otherCard,
                            TextView otherTitle,
                            TextView otherDescription,
                            Activity activity) {
        int surface = themeColor(activity, R.attr.colorSurface);
        int primary = themeColor(activity, R.attr.colorPrimary);
        int onSurface = themeColor(activity, R.attr.colorOnSurface);
        int onPrimary = themeColor(activity, R.attr.colorOnPrimary);

        selectedCard.setCardBackgroundColor(primary);
        selectedCard.setStrokeColor(primary);
        selectedTitle.setTextColor(onPrimary);
        selectedDescription.setTextColor(withAlpha(onPrimary, 190));

        otherCard.setCardBackgroundColor(surface);
        otherCard.setStrokeColor(withAlpha(onSurface, 100));
        otherTitle.setTextColor(withAlpha(onSurface, 100));
        otherDescription.setTextColor(withAlpha(onSurface, 100));
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
        TextView privateTitle = view.findViewById(R.id.card_private_title);
        TextView privateDescriptionText = view.findViewById(R.id.card_private_text);

        MaterialCardView cardDocuments =
                view.findViewById(R.id.card_documents);
        TextView documentsTitle = view.findViewById(R.id.card_documents_title);
        TextView documentsDescriptionText = view.findViewById(R.id.card_documents_description);


        CheckBox checkRemember =
                view.findViewById(R.id.check_remember);

        final StorageLocation[] selected =
                { StorageLocation.APP_PRIVATE };

        cardPrivate.setChecked(true);
        selectCard(cardPrivate,
                privateTitle,
                privateDescriptionText,
                cardDocuments,
                documentsTitle,
                documentsDescriptionText,
                activity);

        cardPrivate.setOnClickListener(v -> {

            selected[0] = StorageLocation.APP_PRIVATE;
            selectCard(cardPrivate,
                    privateTitle,
                    privateDescriptionText,
                    cardDocuments,
                    documentsTitle,
                    documentsDescriptionText,
                    activity);


        });

        cardDocuments.setOnClickListener(v -> {

            selected[0] = StorageLocation.SHARED_DOCUMENTS;
            selectCard(cardDocuments,
                    documentsTitle,
                    documentsDescriptionText,
                    cardPrivate,
                    privateTitle,
                    privateDescriptionText,
                    activity);

        });

        TextView subtitle =
                view.findViewById(R.id.subtitle);

        TextView documentsDescription =
                view.findViewById(R.id.card_documents_description);

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
                        .setNegativeButton("Cancel",
                                (d, which) -> {
                                    boolean rememberChoice =
                                            checkRemember.isChecked();
                                    callback.onLocationSelected(
                                            StorageLocation.CANCELLED,
                                            rememberChoice);
                                })


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

        int buttonColor = Color.rgb(100, 181, 246);

        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setTextColor(buttonColor);

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                .setTextColor(buttonColor);
    }


}