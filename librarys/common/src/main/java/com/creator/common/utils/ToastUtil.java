package com.creator.common.utils;

import android.content.Context;
import android.widget.Toast;

import com.creator.common.MainThreadExecutor;

public class ToastUtil {

    public static void show(Context context, String text) {

        MainThreadExecutor.INSTANCE.runOnUiThread(() -> {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            return null;
        });
    }
}