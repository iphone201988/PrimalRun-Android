package com.primal.runs.utils.recyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.primal.runs.R;


public class CustomRecyclerView  extends RecyclerView {
    private BouncyAdapter mBouncyAdapter;
    private Adapter mOriginalAdapter;
    private BouncyConfig mConfig = BouncyConfig.DEFAULT;

    public CustomRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public CustomRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (mOriginalAdapter != null) {
            mOriginalAdapter.unregisterAdapterDataObserver(mAdapterDataObserver);
        }

        mOriginalAdapter = adapter;
        mBouncyAdapter = new BouncyAdapter(getContext(), this, adapter, mConfig);

        super.setAdapter(mBouncyAdapter);
        adapter.registerAdapterDataObserver(mAdapterDataObserver);
    }

    @Override
    public void swapAdapter(Adapter adapter, boolean removeAndRecycleExistingViews) {
        setAdapter(adapter);
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        if (!(layout instanceof LinearLayoutManager)) {
            throw new RuntimeException("RecyclerView must use LinearLayoutManager");
        }

        super.setLayoutManager(layout);
    }

    @Override
    public void scrollToPosition(int position) {
        super.scrollToPosition(position + 1);
    }

    @Override
    public void smoothScrollToPosition(int position) {
        super.smoothScrollToPosition(position + 1);
    }

    private void init(Context context, AttributeSet attributeSet) {
        if (context != null && attributeSet != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attributeSet,
                    R.styleable.RecyclerViewBouncy,
                    0, 0
            );

            BouncyConfig.Builder builder = new BouncyConfig.Builder();

            if (a.hasValue(R.styleable.RecyclerViewBouncy_tension)) {
                builder.setTension(a.getInteger(R.styleable.RecyclerViewBouncy_tension, 0));
            }

            if (a.hasValue(R.styleable.RecyclerViewBouncy_friction)) {
                builder.setFriction(a.getInteger(R.styleable.RecyclerViewBouncy_friction, 0));
            }

            if (a.hasValue(R.styleable.RecyclerViewBouncy_gapLimit)) {
                builder.setGapLimit(a.getInteger(R.styleable.RecyclerViewBouncy_gapLimit, 0));
            }

            if (a.hasValue(R.styleable.RecyclerViewBouncy_speedFactor)) {
                builder.setSpeedFactor(a.getInteger(R.styleable.RecyclerViewBouncy_speedFactor, 0));
            }

            if (a.hasValue(R.styleable.RecyclerViewBouncy_viewCountEstimateSize)) {
                builder.setViewCountEstimateSize(a.getInteger(
                        R.styleable.RecyclerViewBouncy_viewCountEstimateSize, 0));
            }

            if (a.hasValue(R.styleable.RecyclerViewBouncy_maxAdapterSizeToEstimate)) {
                builder.setMaxAdapterSizeToEstimate(a.getInteger(
                        R.styleable.RecyclerViewBouncy_maxAdapterSizeToEstimate, 0));
            }

            mConfig = builder.build();
        }
    }

    private final AdapterDataObserver mAdapterDataObserver = new AdapterDataObserver() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onChanged() {
            mBouncyAdapter.notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            mBouncyAdapter.notifyItemRangeChanged(positionStart + 1, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            mBouncyAdapter.notifyItemRangeChanged(positionStart + 1, itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mBouncyAdapter.notifyItemRangeInserted(positionStart + 1, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mBouncyAdapter.notifyItemRangeRemoved(positionStart + 1, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mBouncyAdapter.notifyItemMoved(fromPosition + 1, toPosition + 1);
        }
    };
}