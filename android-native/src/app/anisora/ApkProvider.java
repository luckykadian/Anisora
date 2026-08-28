package app.anisora;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;

/**
 * Serves downloaded extension APKs to Android's package installer via
 * content:// URIs (targetSdk 24+ forbids file:// — this replaces FileProvider).
 */
public class ApkProvider extends ContentProvider {

    public boolean onCreate() {
        return true;
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws java.io.FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("/") || name.contains("..")) {
            throw new java.io.FileNotFoundException(String.valueOf(uri));
        }
        File f = new File(new File(getContext().getFilesDir(), "extensions"), name);
        if (!f.exists()) throw new java.io.FileNotFoundException(f.getAbsolutePath());
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        return null;
    }

    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    public int delete(Uri uri, String selection, String[] args) {
        return 0;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        return 0;
    }
}
