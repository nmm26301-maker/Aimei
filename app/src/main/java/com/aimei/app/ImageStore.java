package com.aimei.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.*;
import java.util.*;

final class ImageStore {
    private ImageStore() { }

    static File directory(Context context) throws IOException {
        File folder = new File(context.getFilesDir(), "wardrobe");
        if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("无法创建衣橱");
        return folder;
    }

    static List<File> list(Context context) {
        File folder = new File(context.getFilesDir(), "wardrobe");
        File[] entries = folder.listFiles((parent, name) -> name.matches("[a-f0-9-]{36}\\.jpg"));
        List<File> files = new ArrayList<>();
        if (entries != null) Collections.addAll(files, entries);
        files.sort((left, right) -> Long.compare(right.lastModified(), left.lastModified()));
        return files;
    }

    static File save(Context context, Uri source) throws IOException {
        File temporary = File.createTempFile("aimei-image-", ".tmp", context.getCacheDir());
        try {
            copyLimited(context, source, temporary);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(temporary.getPath(), options);
            if (options.outWidth <= 0 || options.outHeight <= 0) throw new IOException("无法读取这张图片");
            options.inSampleSize = 1;
            while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 2048) {
                options.inSampleSize *= 2;
            }
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(temporary.getPath(), options);
            if (bitmap == null) throw new IOException("图片解码失败");
            File output = new File(directory(context), UUID.randomUUID().toString() + ".jpg");
            try {
                try (OutputStream stream = new FileOutputStream(output)) {
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)) {
                        throw new IOException("图片保存失败");
                    }
                }
                return output;
            } catch (IOException failure) {
                if (!output.delete()) output.deleteOnExit();
                throw failure;
            } finally {
                bitmap.recycle();
            }
        } finally {
            if (!temporary.delete()) temporary.deleteOnExit();
        }
    }

    private static void copyLimited(Context context, Uri source, File destination) throws IOException {
        try (InputStream input = context.getContentResolver().openInputStream(source);
             OutputStream output = new FileOutputStream(destination)) {
            if (input == null) throw new IOException("无法打开图片");
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > 30 * 1024 * 1024) throw new IOException("请选择小于 30 MB 的图片");
                output.write(buffer, 0, read);
            }
        }
    }

    static Bitmap thumbnail(File file, int maximum) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), options);
        options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > maximum) {
            options.inSampleSize *= 2;
        }
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getPath(), options);
    }

    static Uri uri(File file) {
        return Uri.parse("content://com.aimei.app.wardrobe/" + file.getName());
    }
}
