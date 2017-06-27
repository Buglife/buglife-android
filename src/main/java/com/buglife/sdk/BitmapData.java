package com.buglife.sdk;

import android.graphics.Bitmap;

final class BitmapData extends AttachmentData {

    final private Bitmap mBitmap;

    BitmapData(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    Bitmap getBitmap() {
        return mBitmap;
    }

    String getBase64EncodedData() {
        throw new Buglife.BuglifeException("Unimplemented");
    }

}
