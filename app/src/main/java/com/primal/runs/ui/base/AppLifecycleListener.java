package com.primal.runs.ui.base;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;


public class AppLifecycleListener implements DefaultLifecycleObserver {
    Context context;

    public AppLifecycleListener(Context context) {
        this.context = context;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        Log.i("ONSTART", "DefaultLifecycleObserver: ");

    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStop(owner);
        Log.i("onStop", "DefaultLifecycleObserver: ");
    }
}