/*
 * Copyright (C) 2017 Buglife, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.buglife.sdk;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for showing a list of attachment objects in the bug reporter UI
 */
class AttachmentAdapter extends BaseAdapter {
    private ArrayList<Attachment> mDataSource;

    AttachmentAdapter(List<Attachment> attachments) {
        mDataSource = new ArrayList<Attachment>(attachments);
    }

    void setAttachments(List<Attachment> attachments) {
        mDataSource = new ArrayList<>(attachments);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mDataSource.size();
    }

    @Override
    public Attachment getItem(int position) {
        return mDataSource.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.attachment_list_item, parent, false);
        }

        ImageView thumbnailView = (ImageView) convertView.findViewById(com.buglife.sdk.R.id.attachment_list_thumbnail);
        TextView titleView = (TextView) convertView.findViewById(com.buglife.sdk.R.id.attachment_list_title);
        Attachment attachment = getItem(position);

        Context context = convertView.getContext();
        if (attachment instanceof FileAttachment) {
            File file = ((FileAttachment) attachment).getFile();
            if (attachment.isImageAttachment()) {
                String path = file.getAbsolutePath();
                // TODO: Optimize bitmap decoding
                Bitmap scaledBitmap = scaleBitmapForThumbnail(context, BitmapFactory.decodeFile(path));
                thumbnailView.setImageBitmap(scaledBitmap);
            }
            titleView.setText(file.getName());
        }

        return convertView;
    }

    private Bitmap scaleBitmapForThumbnail(Context context, Bitmap bitmap) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        float aspectRatio = (float) originalWidth / (float) originalHeight;

        // 40dp width is the standard size for a row icon according to material design guidelines.
        int scaledWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
        int scaledHeight = (int) (scaledWidth / aspectRatio);

        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false);
    }
}
