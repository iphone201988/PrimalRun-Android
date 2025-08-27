package com.primal.runs.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import com.primal.runs.BR;
import com.primal.runs.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;


public class BaseCustomBottomSheet<V extends ViewDataBinding> extends BottomSheetDialog {

    private Context context;
    private V binding;
    private @LayoutRes
    int layoutId;
    private final Listener listener;

    public BaseCustomBottomSheet(@NonNull Context context, @LayoutRes int layoutId, Listener listener) {
        super(context, R.style.SheetDialog);
        this.context = context;
        this.layoutId = layoutId;
        this.listener = listener;
    }

    public BaseCustomBottomSheet(@NonNull Context context, @LayoutRes int layoutId, @StyleRes int style, Listener listener) {
        super(context, style);
        this.context = context;
        this.layoutId = layoutId;
        this.listener = listener;
    }

    public BaseCustomBottomSheet(@NonNull Context context, @LayoutRes int layoutId, String type, Listener listener) {
        // use when no custom style is required
        super(context, R.style.CustomBottomSheetDialog);
        this.context = context;
        this.layoutId = layoutId;
        this.listener = listener;
    }

    public V getBinding() {
        init();
        return binding;
    }

    private void init() {
        if (binding == null)
            binding = DataBindingUtil.inflate(LayoutInflater.from(context), layoutId, null, false);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (listener != null) binding.setVariable(BR.callback, listener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(binding.getRoot());
    }

    public interface Listener {
        void onViewClick(View view);
    }
}