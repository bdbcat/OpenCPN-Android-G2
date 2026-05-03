package org.opencpn;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import android.provider.OpenableColumns;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class SafManager {

    private static final String TAG = "OpenCPN";
    //private static final int REQUEST_SAF_TREE = 1001;

    //------------------------
    // Request codes
    public final static int OCPN_SAF_DIALOG_FILE_CHOOSER_REQUEST_CODE = 5000;

    private final Activity activity;
    private final NativeBridgeManager nativeBridge;
    private JSONHelpers JsonHelpers;

    private static class PendingRequest {

        int seq;
        String operation;
        String purpose;

        PendingRequest(int seq,
                       String operation,
                       String purpose) {

            this.seq = seq;
            this.operation = operation;
            this.purpose = purpose;
        }
    }


    // --------------------------------------------------------------------
    // Request code allocator
    // --------------------------------------------------------------------

    private int nextRequestCode = 5000;

    // --------------------------------------------------------------------
    // Track pending async SAF requests
    // requestCode -> seq
    // --------------------------------------------------------------------

    private final Map<Integer, PendingRequest> pendingRequests =
            new HashMap<>();
    private String m_wildcard;
    private final SharedPreferences prefs;

    public SafManager(Activity activity,
                      NativeBridgeManager nativeBridge) {

        this.activity = activity;
        this.nativeBridge = nativeBridge;
        this.prefs = activity.getSharedPreferences(
                "opencpn_saf_prefs",
                Activity.MODE_PRIVATE
        );
    }
    //---------------------------------------------------------------------
    //  Utility functions
    //---------------------------------------------------------------------

    private int allocateRequestCode() {

        while (pendingRequests.containsKey(nextRequestCode)) {

            nextRequestCode++;

            if (nextRequestCode > 5999)
                nextRequestCode = 5000;
        }

        int requestCode = nextRequestCode;

        nextRequestCode++;

        if (nextRequestCode > 5999)
            nextRequestCode = 5000;

        return requestCode;
    }
    private String getFileName(Uri uri, String wildcard) {
        // Extract the target file name from Uri
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (fileName == null) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }

        String displayName = fileName;

        boolean baccept = false;
        if (!wildcard.isEmpty()){
            String wildtail = "";

            String[] wildparts = wildcard.split("\\|");
            for (String part : wildparts) {
                if (part.contains("("))
                    continue;
                else
                    wildtail = part.substring(1);  // like ".gpx"

                if (!wildtail.isEmpty()) {
                    if (displayName.endsWith(wildtail)){
                        baccept = true;
                        break;      // the loop
                    }
                    if (wildtail.contains("*")) {
                        baccept = true;
                        break;
                    }
                }
            }
        }

        return displayName;
    }

// --------------------------------------------------------------------
// Ensure persistent access to a SAF tree.
//
// If already granted:
//      immediately post "safTreeReady"
//
// Otherwise:
//      launch ACTION_OPEN_DOCUMENT_TREE
//
// Native side supplies:
//      seq
//      treeId      ("downloads", "documents", etc)
//
// --------------------------------------------------------------------

    public String ensureTreeAccess(int seq, String treeId) {

        try {


            // ------------------------------------------------------------
            // Check existing persisted permission
            // ------------------------------------------------------------

            String storedUriString =
                    prefs.getString(
                            "tree_uri_" + treeId,
                            null
                    );

            if(storedUriString != null) {

                Uri storedUri =
                        Uri.parse(storedUriString);

                for(UriPermission perm :
                        activity.getContentResolver()
                                .getPersistedUriPermissions()) {

                    if(perm.getUri().equals(storedUri)
                            && perm.isReadPermission()) {

                        // --------------------------------------------
                        // Already authorized
                        // --------------------------------------------

                        nativeBridge.postJsonEvent(
                                JsonHelpers.makeEvent(seq,
                                        "safTreeReady",
                                        "treeId",
                                        treeId,
                                        "uri",
                                        storedUri.toString()
                                )
                        );

                        return "NOP";
                    }
                }
            }

            // ------------------------------------------------------------
            // Need user authorization
            // ------------------------------------------------------------

            int requestCode = allocateRequestCode();

            pendingRequests.put(
                    requestCode,
                    new PendingRequest(
                            seq,
                            "TREE_ACCESS",
                            treeId
                    )
            );

            Intent intent =
                    new Intent(
                            Intent.ACTION_OPEN_DOCUMENT_TREE
                    );

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            );

            // ------------------------------------------------------------
            // Optional UX improvement:
            // hint initial location if previously known
            // ------------------------------------------------------------

            if(storedUriString != null) {

                intent.putExtra(
                        DocumentsContract.EXTRA_INITIAL_URI,
                        Uri.parse(storedUriString)
                );
            }

            activity.startActivityForResult(
                    intent,
                    requestCode
            );

            Log.i(TAG,
                    "Launching TREE grant for "
                            + treeId
                            + " seq="
                            + seq);

            return "STARTED";
        }

        catch(Exception e) {

            Log.e(TAG,
                    "ensureTreeAccess exception",
                    e);

            return "EXCEPTION";
        }
    }

    // --------------------------------------------------------------------
    // Launch SAF folder picker
    // --------------------------------------------------------------------

    public String xopenDocument(int seq, String wildcard) {
        openTreePicker();
        return "OK";
    }

    public String openDocument(int seq, String wildcard) {

        m_wildcard = wildcard;

        int requestCode = allocateRequestCode();

        pendingRequests.put(
                requestCode,
                new PendingRequest(
                        seq,
                        "OPEN_DOCUMENT",
                        ""
                )
        );
        try {

            Intent intent =
                    new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            );
            intent.setType("*/*");       // Need this for ACTION_OPEN_DOCUMENT
            //intent.addCategory(Intent.CATEGORY_OPENABLE);;

            String suri = "content://com.android.externalstorage.documents/tree/primary%3ADocuments";
            Uri initialUri = Uri.parse(suri);;
            intent.putExtra(
                    DocumentsContract.EXTRA_INITIAL_URI,
                    initialUri
            );

            activity.startActivityForResult(
                    intent,
                    requestCode );
            return "OK";
        }
        catch(Exception e) {
            Log.e(TAG, "openDocumentr exception", e);
            return "EXCEPTION";
        }
    }

    // --------------------------------------------------------------------
    // Launch SAF folder picker
    // --------------------------------------------------------------------

    public String openTreePicker() {

        try {

            Intent intent =
                    new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                            Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            );

            activity.startActivityForResult(
                    intent,
                    200
            );

            return "OK";

        }
        catch(Exception e) {

            Log.e(TAG, "openTreePicker exception", e);
            return "EXCEPTION";
        }
    }

    // --------------------------------------------------------------------
    // Activity result entry point
    // --------------------------------------------------------------------

    public boolean onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {

        PendingRequest pending =
                pendingRequests.remove(requestCode);

        if(pending == null)
            return false;

        switch(pending.operation) {

            case "OPEN_DOCUMENT":

                HandleFileOpenResult(
                        pending,
                        resultCode,
                        data
                );
                return true;

            case "TREE_ACCESS":

                handleTreeGrantResult(
                        pending,
                        resultCode,
                        data
                );
                return true;

            case "CREATE_DOCUMENT":

                //handleCreateDocumentResult(
                //        pending,
                //        resultCode,
                //        data
                //);
                return true;

            default:

                Log.e(TAG,
                        "Unknown SAF operation: "
                                + pending.operation);

                return false;
        }
    }

    // --------------------------------------------------------------------
    // SAF result processing
    // --------------------------------------------------------------------

    private void handleTreeGrantResult(
            PendingRequest pending,
            int resultCode,
            Intent data) {

        int seq = pending.seq;
        String treeId = pending.purpose;

        if(resultCode != Activity.RESULT_OK
                || data == null
                || data.getData() == null) {

            nativeBridge.postJsonEvent(
                    JsonHelpers.makeEvent(
                            seq,
                            "safTreeDenied",
                             "treeId",
                            treeId
                    )
            );

            return;
        }

        Uri treeUri = data.getData();

        final int takeFlags =
                data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        activity.getContentResolver()
                .takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );

        // Persist URI string
        prefs.edit()
                .putString(
                        "tree_uri_" + treeId,
                        treeUri.toString()
                )
                .apply();

        nativeBridge.postJsonEvent(
                JsonHelpers.makeEvent(
                        seq,
                        "safTreeReady",
                        "treeId",
                        treeId,
                        "uri",
                        treeUri.toString()
                )
        );
    }

    private void HandleFileOpenResult(PendingRequest pending, int resultCode, Intent data) {
        int seq = pending.seq;
        try {

            if(resultCode != Activity.RESULT_OK) {

                nativeBridge.postJsonEvent(
                        JSONHelpers.makeEvent(seq,
                                "safFileOpenCancelled"
                        )
                );

                return;
            }

            if(data == null) {
                nativeBridge.postJsonEvent(
                        JSONHelpers.makeEvent(seq,
                                "safFileOpenError",
                                "reason",
                                "null_intent"
                        )
                );
                return;
            }

            Uri fileUri = data.getData();
            if(fileUri == null) {
                nativeBridge.postJsonEvent(
                        JSONHelpers.makeEvent(seq,
                                "safFileOpenError",
                                "reason",
                                "null_uri"
                        )
                );
                return;
            }

            // ------------------------------------------------------------
            // Persist permissions immediately
            // ------------------------------------------------------------

            final int takeFlags =
                    data.getFlags()
                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            ContentResolver resolver =
                    activity.getContentResolver();

            resolver.takePersistableUriPermission(
                    fileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            // ------------------------------------------------------------
            // Notify native side
            // ------------------------------------------------------------

            String fileName = getFileName(fileUri, m_wildcard);
            nativeBridge.postJsonEvent(
                    JSONHelpers.makeEvent(seq,
                            "safFileOpenSelected",
                            "uri",
                            fileUri.toString(),
                            "filename",
                            fileName

                    )
            );

        }
        catch(Exception e) {

            Log.e(TAG,
                    "HandleFileOpenResult exception",
                    e);

            nativeBridge.postJsonEvent(
                    JSONHelpers.makeEvent(seq,
                            "safFileOpenError",
                            "reason",
                            "exception"
                    )
            );
        }
    }





    private void handleTreePickerResult(int resultCode,
                                        Intent data) {

        try {

            if(resultCode != Activity.RESULT_OK) {

                nativeBridge.postJsonEvent(
                        JSONHelpers.makeEvent(
                                50,
                                "safTreeCancelled"
                        )
                );

                return;
            }

            if(data == null) {

                nativeBridge.postJsonEvent(
                        JSONHelpers.makeEvent(51,
                                "safTreeError",
                                "reason",
                                "null_intent"
                        )
                );

                return;
            }

            Uri treeUri = data.getData();

            if(treeUri == null) {

                nativeBridge.postJsonEvent(
                        JSONHelpers.makeEvent(52,
                                "safTreeError",
                                "reason",
                                "null_uri"
                        )
                );

                return;
            }

            // ------------------------------------------------------------
            // Persist permissions immediately
            // ------------------------------------------------------------

            final int takeFlags =
                    data.getFlags()
                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            ContentResolver resolver =
                    activity.getContentResolver();

            resolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            // ------------------------------------------------------------
            // Notify native side
            // ------------------------------------------------------------

            nativeBridge.postJsonEvent(
                    JSONHelpers.makeEvent(57,
                            "safTreeSelected",
                            "uri",
                            treeUri.toString()
                    )
            );

        }
        catch(Exception e) {

            Log.e(TAG,
                    "handleTreePickerResult exception",
                    e);

            nativeBridge.postJsonEvent(
                    JSONHelpers.makeEvent(54,
                            "safTreeError",
                            "reason",
                            "exception"
                    )
            );
        }
    }
}