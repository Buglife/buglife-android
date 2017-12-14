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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.buglife.sdk.reporting.ReportSubmissionCallback;

import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static com.buglife.sdk.ActivityUtils.INTENT_KEY_ATTACHMENT;
import static com.buglife.sdk.ActivityUtils.INTENT_KEY_BUG_CONTEXT;

public class ReportActivity extends AppCompatActivity {

    private static final int SEND_MENU_ITEM = 1;

    private BugContext mBugContext;
    private AttachmentAdapter mAttachmentAdapter;
    private ListView mAttachmentListView;
    private @NonNull List<InputField> mInputFields;
    private @Nullable ProgressDialog mProgressDialog;
    private @NonNull ColorPalette mColorPalette;

    public static Intent newStartIntent(Context context, BugContext bugContext) {
        Intent intent = new Intent(context, ReportActivity.class);
        intent.setFlags(intent.getFlags() | FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(INTENT_KEY_BUG_CONTEXT, bugContext);
        return intent;
    }

    public ReportActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        mAttachmentListView = (ListView) findViewById(R.id.attachment_list_view);

        Intent intent = getIntent();
        mBugContext = intent.getParcelableExtra(INTENT_KEY_BUG_CONTEXT);

        final List<FileAttachment> mediaAttachments = mBugContext.getMediaAttachments();

        mAttachmentAdapter = new AttachmentAdapter(mediaAttachments);
        mAttachmentListView.setAdapter(mAttachmentAdapter);
        mAttachmentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FileAttachment attachment = mAttachmentAdapter.getItem(position);
                showActivityForAttachment(attachment);
            }
        });

        mInputFields = Buglife.getInputFields();
        ArrayList<InputFieldView> inputFieldViews = new ArrayList<>();

        LinearLayout inputFieldLayout = (LinearLayout) findViewById(R.id.input_field_layout);

        for (final InputField inputField : mInputFields) {
            final InputFieldView inputFieldView = InputFieldView.newInstance(this, inputField);
            final String currentValue = getValueForInputField(inputField);

            inputFieldView.configureWithInputField(inputField, new InputFieldView.ValueCoordinator() {
                @Override
                public void onValueChanged(@NonNull InputField inputField, @Nullable String newValue) {
                    setValueForInputField(inputField, newValue);
                }
            });

            inputFieldLayout.addView(inputFieldView);
            inputFieldViews.add(inputFieldView);
            inputFieldView.setValue(currentValue);
        }

        mColorPalette = new ColorPalette.Builder(this).build();
        int colorPrimary = mColorPalette.getColorPrimary();
        int titleTextColor = mColorPalette.getTextColorPrimary();
        String titleTextColorHex = ColorPalette.getHexColor(titleTextColor);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.parseColor(titleTextColorHex));
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            Drawable drawable = ActivityUtils.getTintedDrawable(this, android.R.drawable.ic_menu_close_clear_cancel, mColorPalette.getTextColorPrimary());

            actionBar.setHomeAsUpIndicator(drawable);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setBackgroundDrawable(new ColorDrawable(colorPrimary));
            actionBar.setTitle(getString(R.string.report_a_bug));
        }

        ActivityUtils.setStatusBarColor(this, mColorPalette.getColorPrimaryDark());
    }

    private @Nullable String getValueForInputField(@NonNull InputField inputField) {
        String attributeName = inputField.getAttributeName();
        return mBugContext.getAttribute(attributeName);
    }

    void setValueForInputField(@NonNull InputField inputField, @Nullable String value) {
        String attributeName = inputField.getAttributeName();
        mBugContext.putAttribute(attributeName, value);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem sendItem = menu.add(0, SEND_MENU_ITEM, Menu.NONE, R.string.send);
        sendItem.setShowAsAction(SHOW_AS_ACTION_ALWAYS);
        Drawable drawable = ActivityUtils.getTintedDrawable(this, android.R.drawable.ic_menu_send, mColorPalette.getTextColorPrimary());
        sendItem.setIcon(drawable);

        return true;
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

    private void showActivityForAttachment(FileAttachment attachment) {
        if (attachment.isImage()) {
            showScreenshotAnnotatorActivity(attachment);
        } else if (attachment.isVideo()) {
            showVideoActivity(attachment);
        }
    }

    private void showScreenshotAnnotatorActivity(FileAttachment attachment) {
        Intent intent = ScreenshotAnnotatorActivity.newStartIntent(this, attachment);
        startActivityForResult(intent, ScreenshotAnnotatorActivity.REQUEST_CODE);
    }

    private void showVideoActivity(FileAttachment attachment) {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra(INTENT_KEY_ATTACHMENT, attachment);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ScreenshotAnnotatorActivity.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mAttachmentAdapter.notifyDataSetChanged();
            }
        }
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

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }

    private void submitReport() {
        Report report = new Report(mBugContext);

        if (Buglife.getRetryPolicy() == RetryPolicy.MANUAL) {
            showProgressDialog();
        }

        Buglife.submitReport(report, new ReportSubmissionCallback() {
            @Override
            public void onSuccess() {
                dismissProgressDialog();
                Toast.makeText(ReportActivity.this, R.string.thanks_for_filing_a_bug, Toast.LENGTH_SHORT).show();
                dismiss();
            }

            @Override
            public void onFailure(Error error, Throwable throwable) {
                dismissProgressDialog();
                throwable.printStackTrace();

                switch (error) {
                    case NETWORK:
                        showErrorDialog(getString(R.string.error_dialog_message));
                        break;
                    case SERIALIZATION:
                        showErrorDialog(getString(R.string.error_dialog_message_check_logs));
                        break;
                }
            }
        });
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(this, getString(R.string.sending_toast), "");
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void showErrorDialog(String message) {
        final AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.buglife_alert_dialog).create();
        alertDialog.setTitle(R.string.error_dialog_title);
        alertDialog.setMessage(getString(R.string.error_dialog_message));
        alertDialog.setMessage(message);
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

