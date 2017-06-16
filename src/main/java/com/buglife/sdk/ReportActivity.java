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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.RequestQueue;

import java.util.List;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

public class ReportActivity extends AppCompatActivity {

    static final String INTENT_KEY_BUG_CONTEXT = "INTENT_KEY_BUG_CONTEXT";
    private static final int SEND_MENU_ITEM = 1;

    private BugContext mBugContext;
    private AttachmentAdapter mAttachmentAdapter;
    private ListView mAttachmentListView;
    private EditText mWhatHappenedEditText;
    private ProgressDialog mProgressDialog;
    private RequestQueue mRequestQueue;

    public ReportActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        mWhatHappenedEditText = (EditText) findViewById(R.id.what_happened_edit_text);
        mAttachmentListView = (ListView) findViewById(R.id.attachment_list_view);

        Intent intent = getIntent();
        mBugContext = intent.getParcelableExtra(INTENT_KEY_BUG_CONTEXT);

        final List<Attachment> attachments = mBugContext.getAttachments();

        mAttachmentAdapter = new AttachmentAdapter(this, attachments);
        mAttachmentListView.setAdapter(mAttachmentAdapter);
        mAttachmentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Attachment attachment = mAttachmentAdapter.getItem(position);
                showScreenshotAnnotatorActivity(attachment);
            }
        });

        int colorPrimary = Buglife.getColorPalette().getColorPrimary();
        int titleTextColor = Buglife.getColorPalette().getTextColorPrimary();
        String titleTextColorHex = ColorPalette.getHexColor(titleTextColor);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            Drawable drawable = getTintedDrawable(android.R.drawable.ic_menu_close_clear_cancel);

            actionBar.setHomeAsUpIndicator(drawable);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setBackgroundDrawable(new ColorDrawable(colorPrimary));
            actionBar.setTitle(Html.fromHtml("<font color='" + titleTextColorHex + "'>" + getString(R.string.report_a_bug) + "</font>"));
        }

        ActivityUtils.setStatusBarColor(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem sendItem = menu.add(0, SEND_MENU_ITEM, Menu.NONE, R.string.send);
        sendItem.setShowAsAction(SHOW_AS_ACTION_ALWAYS);
        Drawable drawable = getTintedDrawable(android.R.drawable.ic_menu_send);
        sendItem.setIcon(drawable);

        return true;
    }

    private Drawable getTintedDrawable(int id) {
        int titleTextColor = Buglife.getColorPalette().getTextColorPrimary();
        Drawable drawable = getResources().getDrawable(id);
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, titleTextColor);
        return drawable;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case SEND_MENU_ITEM:
                submitReport();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showScreenshotAnnotatorActivity(Attachment attachment) {
        Intent intent = new Intent(this, ScreenshotAnnotatorActivity.class);
        intent.putExtra(ScreenshotAnnotatorActivity.INTENT_KEY_ATTACHMENT, attachment);
        startActivityForResult(intent, ScreenshotAnnotatorActivity.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ScreenshotAnnotatorActivity.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Attachment attachment = data.getParcelableExtra(ScreenshotAnnotatorActivity.INTENT_KEY_ATTACHMENT);
                mBugContext.updateAttachment(attachment);
                mAttachmentAdapter.setAttachments(mBugContext.getAttachments());
            }
        }
    }

    /**
     * Buton tap handlers
     */

    public void cancelButtonPressed(View view) {
        dismiss();
    }

    public void sendButtonPressed(MenuItem item) {
        submitReport();
    }

    private void dismiss() {
        onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        Buglife.onFinishReportFlow();
    }

    private void submitReport() {
        final Context context = this;
        final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.sending_toast), "");

        String whatHappened = mWhatHappenedEditText.getText().toString();
        Report report = new Report(mBugContext, whatHappened);
        Buglife.submitReport(report, new RequestHandler() {
            @Override
            public void onSuccess() {
                progressDialog.dismiss();
                Toast.makeText(context, R.string.thanks_for_filing_a_bug, Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onFailure(Throwable e) {
                Log.d("Error submitting report", e);
                progressDialog.dismiss();
                showErrorDialog();
            }
        });
    }

    private void showErrorDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.buglife_alert_dialog).create();
        alertDialog.setTitle(R.string.error_dialog_title);
        alertDialog.setMessage(getString(R.string.error_dialog_message));
        alertDialog.setCancelable(false);

        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }
}

