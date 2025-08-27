package com.primal.runs.ui.dashboard.home

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.primal.runs.databinding.ItemPlansHomeBinding
import com.primal.runs.ui.dashboard.home.model.GroupedPlanData

class PlansAdapter(val context:Context, val list:ArrayList<GroupedPlanData>, private val clickListener:OnPlanClickListener
):RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
      return PlanHomeHolder(ItemPlansHomeBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun getItemCount(): Int {
      return (list.size)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        Log.d("colorMatrix", "onBindViewHolder: colorMatrix $item")
        (holder as PlanHomeHolder).binding.apply {
            bean=item

            if(item.isPremium == true){
                Log.d("colorMatrix", "onBindViewHolder: colorMatrix")
                val colorMatrix = ColorMatrix()
                colorMatrix.setSaturation(0f) // 0 means grayscale, 1 means original colors

                val filter = ColorMatrixColorFilter(colorMatrix)
                ivElephant.colorFilter = filter
            }

            tvStartPlan.setOnClickListener {
                clickListener.onPlanClick(position, item._id.toString())
            }
        }
    }

    class PlanHomeHolder(itemView: ItemPlansHomeBinding) :
        RecyclerView.ViewHolder(itemView.root) {
        val binding = itemView
    }

    interface OnPlanClickListener {
        fun onPlanClick(position: Int, plan: String)
    }
}
