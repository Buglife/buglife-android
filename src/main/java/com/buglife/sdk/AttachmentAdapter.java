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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for showing a list of attachment objects in the bug reporter UI
 */
class AttachmentAdapter extends BaseAdapter {
    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<Attachment> mDataSource;

    AttachmentAdapter(Context context, List<Attachment> attachments) {
        mContext = context;
        mDataSource = new ArrayList<Attachment>(attachments);
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        View rowView = mInflater.inflate(R.layout.attachment_list_item, parent, false);

        ImageView thumbnailView = (ImageView) rowView.findViewById(com.buglife.sdk.R.id.attachment_list_thumbnail);
        TextView titleView = (TextView) rowView.findViewById(com.buglife.sdk.R.id.attachment_list_title);

        Attachment attachment = getItem(position);

        thumbnailView.setImageBitmap(attachment.getBitmap(rowView.getContext()));
        titleView.setText(attachment.getFilename());

        return rowView;
    }
}
