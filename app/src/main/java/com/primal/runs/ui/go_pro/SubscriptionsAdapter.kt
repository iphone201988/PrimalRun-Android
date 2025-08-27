package com.primal.runs.ui.go_pro

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.primal.runs.R
import com.primal.runs.databinding.SusbItemViewBinding
import com.primal.runs.ui.go_pro.model.SubsModel

class SubscriptionsAdapter(
    val context: Context,
    var item: ArrayList<SubsModel>,
    private val clickListener: ClickListener
) : RecyclerView.Adapter<SubscriptionsAdapter.ViewHolder>() {

    private var selectedId: Int = 0

    class ViewHolder(itemView: SusbItemViewBinding) : RecyclerView.ViewHolder(itemView.root) {
        val binding = itemView

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(SusbItemViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }
    override fun getItemCount(): Int {
        return item.size
    }

    override fun onBindViewHolder(holder: ViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val model = item[position]
        holder.binding.apply {

            tvPrice.text = model.price
            tvDuration.text = model.duration

            if (model.selected) {
                main.setBackgroundResource(R.drawable.bg_pro_rounded)
                main.backgroundTintList = null
                tvDiscount.visibility = View.VISIBLE

                tvDuration.setTextColor(
                    ContextCompat.getColorStateList(
                        context,
                        R.color.colorAccent
                    )
                )
                tvPrice.setTextColor(ContextCompat.getColorStateList(context, R.color.colorAccent))
                tvTitle.setTextColor(ContextCompat.getColorStateList(context, R.color.colorAccent))
            }
            else {
                main.setBackgroundResource(R.drawable.corner_radius_10)
                main.backgroundTintList = context.getColorStateList(R.color.bg_pro)
                tvDiscount.visibility = View.GONE

                tvDuration.setTextColor(ContextCompat.getColorStateList(context, R.color.rating))
                tvPrice.setTextColor(ContextCompat.getColorStateList(context, R.color.rating))
                tvTitle.setTextColor(ContextCompat.getColorStateList(context, R.color.rating))
            }

            main.setOnClickListener {
                item[selectedId].selected = false
                selectedId = position
                model.selected = true
                clickListener.onItemCLick(model)
            }
        }
    }

    interface ClickListener {
        fun onItemCLick(model: SubsModel)
    }
}