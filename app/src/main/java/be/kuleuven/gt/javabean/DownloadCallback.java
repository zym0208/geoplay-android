package be.kuleuven.gt.javabean;

import android.graphics.Bitmap;

public interface DownloadCallback {
    void onImageDownloaded(Bitmap bitmap);
}
