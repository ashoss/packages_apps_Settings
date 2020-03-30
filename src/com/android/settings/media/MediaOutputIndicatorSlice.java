/*
 * Copyright (C) 2019 The Android Open Source Project
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
 */

package com.android.settings.media;

import static com.android.settings.slices.CustomSliceRegistry.MEDIA_OUTPUT_INDICATOR_SLICE_URI;

import android.annotation.ColorInt;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.session.MediaController;
import android.net.Uri;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.SliceAction;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.slices.CustomSliceable;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settings.slices.SliceBroadcastReceiver;
import com.android.settingslib.media.MediaOutputSliceConstants;

public class MediaOutputIndicatorSlice implements CustomSliceable {

    private static final String TAG = "MediaOutputIndSlice";

    private Context mContext;
    private MediaOutputIndicatorWorker mWorker;

    public MediaOutputIndicatorSlice(Context context) {
        mContext = context;
    }

    @Override
    public Slice getSlice() {
        if (!isVisible()) {
            return new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                    .setIsError(true)
                    .build();
        }
        final IconCompat icon = IconCompat.createWithResource(mContext,
                com.android.internal.R.drawable.ic_settings_bluetooth);
        final CharSequence title = mContext.getText(R.string.media_output_title);
        final SliceAction primarySliceAction = SliceAction.createDeeplink(
                getBroadcastIntent(), icon, ListBuilder.ICON_IMAGE, title);
        @ColorInt final int color = Utils.getColorAccentDefaultColor(mContext);
        // To set an empty icon to indent the row
        final ListBuilder listBuilder = new ListBuilder(mContext, getUri(), ListBuilder.INFINITY)
                .setAccentColor(color)
                .addRow(new ListBuilder.RowBuilder()
                        .setTitle(title)
                        .setTitleItem(createEmptyIcon(), ListBuilder.ICON_IMAGE)
                        .setSubtitle(getWorker().getCurrentConnectedMediaDevice().getName())
                        .setPrimaryAction(primarySliceAction));
        return listBuilder.build();
    }

    private IconCompat createEmptyIcon() {
        final Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        return IconCompat.createWithBitmap(bitmap);
    }

    private PendingIntent getBroadcastIntent() {
        final Intent intent = new Intent(getUri().toString());
        intent.setClass(mContext, SliceBroadcastReceiver.class);
        return PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public Uri getUri() {
        return MEDIA_OUTPUT_INDICATOR_SLICE_URI;
    }

    @Override
    public Intent getIntent() {
        // This Slice reflects active media device information and launch MediaOutputSlice. It does
        // not contain its owned Slice data
        return null;
    }

    @Override
    public Class getBackgroundWorkerClass() {
        return MediaOutputIndicatorWorker.class;
    }

    @Override
    public void onNotifyChange(Intent i) {
        final MediaController mediaController = getWorker().getActiveLocalMediaController();
        final Intent intent = new Intent()
                .setAction(MediaOutputSliceConstants.ACTION_MEDIA_OUTPUT)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (mediaController != null) {
            intent.putExtra(MediaOutputSliceConstants.KEY_MEDIA_SESSION_TOKEN,
                    mediaController.getSessionToken());
            intent.putExtra(MediaOutputSliceConstants.EXTRA_PACKAGE_NAME,
                    mediaController.getPackageName());
        }
        mContext.startActivity(intent);
    }

    private MediaOutputIndicatorWorker getWorker() {
        if (mWorker == null) {
            mWorker = SliceBackgroundWorker.getInstance(getUri());
        }
        return mWorker;
    }

    private boolean isVisible() {
        // To decide Slice's visibility.
        // Return true if
        // 1. AudioMode is not in on-going call
        // 2. worker is not null
        // 3. Available devices are more than 0
        return getWorker() != null
                && !com.android.settingslib.Utils.isAudioModeOngoingCall(mContext)
                && getWorker().getMediaDevices().size() > 0;
    }
}
