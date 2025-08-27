package com.primal.runs.ui.profile

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.primal.runs.BR
import com.primal.runs.R
import com.primal.runs.data.model.GetMyActivitiesData
import com.primal.runs.databinding.FragmentAchivementsBinding
import com.primal.runs.databinding.ItemAchievementBinding
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.base.SimpleRecyclerViewAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AchievementsFragment : BaseFragment<FragmentAchivementsBinding>() {
    private val viewModel: ProfileVm by viewModels()
    private lateinit var adapterAchievement: SimpleRecyclerViewAdapter<GetMyActivitiesData, ItemAchievementBinding>

    var myFinalList: ArrayList<GetMyActivitiesData> = ArrayList()


    override fun getLayoutResource(): Int {
        return R.layout.fragment_achivements
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        initOnClick()
        initAchievementAdapter()
        init()
    }

    private fun init(){
        val data  = arguments?.getSerializable("achievements")
        if (data != null) {
            myFinalList = data as ArrayList<GetMyActivitiesData>
            adapterAchievement.list = myFinalList
        }
    }

    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner, Observer {
            when (it?.id) {
                R.id.ivBack -> {
                    super.onBackPressed()
                }
            }
        })
    }

    private fun initAchievementAdapter() {
        adapterAchievement =
            SimpleRecyclerViewAdapter(R.layout.item_achievement, BR.bean) { v, m, pos ->
                when (v.id) {
                    R.id.ivCancel -> {
                        //  requireActivity().onBackPressedDispatcher.onBackPressed()
                        // startActivity(Intent(requireContext(), CommonPageActivity::class.java))
                    }

                }
            }
        binding.rvAchievements.adapter = adapterAchievement

    }
}