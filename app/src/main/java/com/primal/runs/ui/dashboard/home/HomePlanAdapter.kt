package com.primal.runs.ui.dashboard.home

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.LoadAdError
import com.google.gson.Gson
import com.primal.runs.databinding.ItemLastposBinding
import com.primal.runs.databinding.ItemPlanViewBinding
import com.primal.runs.ui.dashboard.home.model.GroupedData
import com.primal.runs.ui.dashboard.home.model.GroupedPlanData
import com.primal.runs.utils.AdManager

class HomePlanAdapter(
    val context: Context,
    var list: ArrayList<GroupedData>,
    val units : String,
    private val clickListener: OnPlanClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_NORMAL = 0
    private val TYPE_LAST = 1
    private lateinit var planAdapter: PlansAdapter

    override fun getItemViewType(position: Int): Int {
        return if (position == list.size - 1) TYPE_LAST else TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_LAST) {
            // Inflate the last item layout
            LastPlanViewHolder(
                ItemLastposBinding.inflate(LayoutInflater.from(context), parent, false)
            )
        } else {
            // Inflate the normal item layout
            PlanViewHolder(
                ItemPlanViewBinding.inflate(LayoutInflater.from(context), parent, false)
            )
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]

        if (holder is PlanViewHolder) {
            // For normal items
            holder.binding.apply {
                bean = item

                // Load ad only once, and set the ad size once
                /*if (adManager.adSize == null) {
                    adManager.loadAd(object : AdManager.AdManagerCallback {
                        override fun onAdLoaded() {
                            consAds.visibility = View.VISIBLE
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            consAds.visibility = View.GONE
                        }

                        override fun onAdClicked() {

                        }
                    })
                }*/

                planAdapter = PlansAdapter(
                    context = context,
                    list = (list[position].data?.map { it.copy(measureUnits = item.measureUnits) }) as ArrayList<GroupedPlanData>,
                    clickListener = object : PlansAdapter.OnPlanClickListener {
                        override fun onPlanClick(position: Int, plan: String) {
                            clickListener.onPlanClicked(position, plan)
                        }
                    }
                )

                rvPlanView.adapter = planAdapter
                rvPlanView.clipToPadding = false
                rvPlanView.clipChildren = false
                if (position == list.size - 1) {
                    rvPlanView.updatePadding(left = 0, right = 0)
                } else {
                    rvPlanView.updatePadding(left = 0, right = 20)
                }
            }
        } else if (holder is LastPlanViewHolder) {
            // For the last item
            holder.binding.apply {
                consHow.setOnClickListener {
                    clickListener.onPlanClicked(position, "LastPlan")
                }
            }
        }
    }

    class PlanViewHolder(itemView: ItemPlanViewBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        val binding = itemView
    }

    class LastPlanViewHolder(itemView: ItemLastposBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        val binding = itemView
    }

    // Interface for handling click events
    interface OnPlanClickListener {
        fun onPlanClicked(position: Int, plan: String)
    }
}
