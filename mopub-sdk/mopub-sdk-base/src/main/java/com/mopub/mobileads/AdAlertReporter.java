// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.mopub.common.AdReport;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.DateAndTime;
import com.mopub.common.util.Intents;
import com.mopub.exceptions.IntentNotResolvableException;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class AdAlertReporter {
    private static final String EMAIL_RECIPIENT = "creative-review@mopub.com";
    private static final String DATE_FORMAT_PATTERN = "M/d/yy hh:mm:ss a z";
    private static final int IMAGE_QUALITY = 25;
    private static final String BODY_SEPARATOR = "\n=================\n";
    @VisibleForTesting
    static final String MESSAGE = "Thank you for taking the time to tell us about your ad experience.\n" +
            "\n" +
            "Please share with us how the ad experience was poor:\n" +
            "\n";

    private final String mDateString;

    private final View mView;
    @NonNull
    private final Context mContext;
    private Intent mEmailIntent;
    private String mParameters;
    private String mResponse;

    public AdAlertReporter(@NonNull final Context context, final View view, @Nullable final AdReport adReport) {
        Preconditions.checkNotNull(context);

        mView = view;
        mContext = context;

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US);
        mDateString = dateFormat.format(DateAndTime.now());

        initEmailIntent();
        Bitmap screenShot = takeScreenShot();
        String screenShotString = convertBitmapInWEBPToBase64EncodedString(screenShot);
        mParameters = "";
        mResponse = "";
        if (adReport != null) {
            mParameters = adReport.toString();
            mResponse = adReport.getResponseString();
        }

        addEmailSubject();
        addEmailBody(mParameters, mResponse, screenShotString);
    }

    public void send() {
        try {
            Intents.startActivity(mContext, mEmailIntent);
        } catch (IntentNotResolvableException e) {
            final Context applicationContext = mContext.getApplicationContext();
            if (applicationContext != null) {
                Toast.makeText(applicationContext, "No email client available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initEmailIntent() {
        mEmailIntent = new Intent(Intent.ACTION_SENDTO);
        // Should not set type since that either overrides (via setType) or conflicts with
        // (via setDataAndType) the data, resulting in NO applications being able to handle this
        // intent.
        mEmailIntent.setData(Uri.parse("mailto:" + EMAIL_RECIPIENT));
    }

    private Bitmap takeScreenShot() {
        if (mView == null || mView.getRootView() == null) {
            return null;
        }

        View rootView = mView.getRootView();
        boolean wasDrawingCacheEnabled = rootView.isDrawingCacheEnabled();
        rootView.setDrawingCacheEnabled(true);

        Bitmap drawingCache = rootView.getDrawingCache();
        if (drawingCache == null) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(drawingCache);
        rootView.setDrawingCacheEnabled(wasDrawingCacheEnabled);

        return bitmap;
    }

    private String convertBitmapInWEBPToBase64EncodedString(Bitmap bitmap) {
        String result = null;
        if (bitmap != null) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, byteArrayOutputStream);
                byte[] bytes = byteArrayOutputStream.toByteArray();
                result = Base64.encodeToString(bytes, Base64.DEFAULT);
            } catch (Exception e) {
                // should we log something here?
            }
        }
        return result;
    }

    private void addEmailSubject() {
        mEmailIntent.putExtra(Intent.EXTRA_SUBJECT, "New creative violation report - "
                + mDateString);
    }

    private void addEmailBody(String... data) {
        StringBuilder body = new StringBuilder();
        body.append(MESSAGE);
        body.append(BODY_SEPARATOR);

        int i = 0;
        while (i<data.length) {
            body.append(data[i]);
            if (i!=data.length-1) {
                body.append(BODY_SEPARATOR);
            }
            i++;
        }
        mEmailIntent.putExtra(Intent.EXTRA_TEXT, body.toString());
    }

    @Deprecated // for testing
    Intent getEmailIntent() {
        return mEmailIntent;
    }

    @Deprecated // for testing
    String getParameters() {
        return mParameters;
    }

    @Deprecated
    String getResponse(){
        return mResponse;
    }
}

