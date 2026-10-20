package com.aimei.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class WardrobeProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    private File resolve(Uri uri) throws FileNotFoundException {
        if (getContext() == null || uri == null
                || !"com.aimei.app.wardrobe".equals(uri.getAuthority())
                || uri.getPathSegments().size() != 1) {
            throw new FileNotFoundException("Invalid image URI");
        }
        String name = uri.getLastPathSegment();
        if (name == null || !name.matches("[a-f0-9-]{36}\\.jpg")) {
            throw new FileNotFoundException("Invalid image name");
        }
        try {
            File folder = ImageStore.directory(getContext()).getCanonicalFile();
            File file = new File(folder, name).getCanonicalFile();
            if (!folder.equals(file.getParentFile()) || !file.isFile()) {
                throw new FileNotFoundException("Image unavailable");
            }
            return file;
        } catch (IOException exception) {
            throw new FileNotFoundException(exception.getMessage());
        }
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Images are read-only");
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public String getType(Uri uri) { return "image/jpeg"; }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) {
        try {
            File image = resolve(uri);
            String[] columns = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
            MatrixCursor cursor = new MatrixCursor(columns);
            Object[] row = new Object[columns.length];
            for (int index = 0; index < columns.length; index++) {
                if (OpenableColumns.DISPLAY_NAME.equals(columns[index])) row[index] = image.getName();
                if (OpenableColumns.SIZE.equals(columns[index])) row[index] = image.length();
            }
            cursor.addRow(row);
            return cursor;
        } catch (FileNotFoundException exception) {
            return null;
        }
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException();
    }
}
