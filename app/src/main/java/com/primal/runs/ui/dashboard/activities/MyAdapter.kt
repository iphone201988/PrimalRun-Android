package com.primal.runs.ui.dashboard.activities

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.LoadAdError
import com.primal.runs.R
import com.primal.runs.data.model.DisplayItem
import com.primal.runs.data.model.PreviousResult
import com.primal.runs.utils.AdManager
import com.primal.runs.utils.ImageUtils.formatDate

class MyAdapter(private val onActivityClick: (PreviousResult) -> Unit) :
    ListAdapter<DisplayItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<DisplayItem>() {
            override fun areItemsTheSame(oldItem: DisplayItem, newItem: DisplayItem): Boolean {
                return oldItem.header == newItem.header && oldItem.activity?.createdAt == newItem.activity?.createdAt
            }

            override fun areContentsTheSame(oldItem: DisplayItem, newItem: DisplayItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isHeader) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            HeaderViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
            )
        } else {
            ActivityViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_months, parent, false),
                onActivityClick
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is HeaderViewHolder) {
            holder.bind(item.header)
        } else if (holder is ActivityViewHolder) {
            holder.bind(item.activity, position)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(header: String?) {
            itemView.findViewById<TextView>(R.id.tvHeader).text = header
        }
    }

    class ActivityViewHolder(
        itemView: View,
        private val onActivityClick: (PreviousResult) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun bind(activity: PreviousResult?, position: Int) {
            if (activity == null) return

            itemView.findViewById<TextView>(R.id.tvDistance).text = "${activity.distance} km"
            itemView.findViewById<TextView>(R.id.tvTime).text = "${activity.duration} min"
            itemView.findViewById<TextView>(R.id.tvRunValue).text = "${activity.averageSpeed} /km"
            try {
                itemView.findViewById<TextView>(R.id.tvTimeDate).text =
                    formatDate(activity.createdAt!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (activity.score != null) {
                itemView.findViewById<RatingBar>(R.id.ratingBar).rating = activity.score.toFloat()
            }

            /*val adManager=  itemView.findViewById<AdManager>(R.id.adManager)
            if (adManager.adSize == null) {
                adManager.loadAd(object : AdManager.AdManagerCallback {
                    override fun onAdLoaded() {

                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                    }

                    override fun onAdClicked() {

                    }
                })
            }*/

            /*if (position % 3 == 2) {  // Position 0 is the first item, 1 is the second, so 3rd item is 2
                itemView.findViewById<ConstraintLayout>(R.id.consAds).visibility = View.VISIBLE
                Log.d("BindingAdapter", "Showing view at position $position")
            } else {
                itemView.findViewById<ConstraintLayout>(R.id.consAds).visibility = View.GONE
                Log.d("BindingAdapter", "Hiding view at position $position")
            }*/

            // Set click listener
            itemView.setOnClickListener {
                onActivityClick(activity)
            }
        }
    }
}
