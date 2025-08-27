package com.primal.runs.ui.go_pro

import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetailsParams
import com.primal.runs.R
import com.primal.runs.databinding.FragmentGoProBinding
import com.primal.runs.ui.base.BaseFragment
import com.primal.runs.ui.base.BaseViewModel
import com.primal.runs.ui.go_pro.model.SubsModel
import com.primal.runs.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class GoProFragment : BaseFragment<FragmentGoProBinding>(), SubscriptionsAdapter.ClickListener, SubscriptionManager.PurchaseListener {
    private val viewModel: GoProVm by viewModels()

    private val subsList : ArrayList<SubsModel> = ArrayList()
    private lateinit var subsAdapter :SubscriptionsAdapter
    private lateinit var subscriptionManager: SubscriptionManager

    private val listOfSUKS = listOf("android.test.purchased")

    override fun getLayoutResource(): Int {
        return R.layout.fragment_go_pro
    }

    override fun getViewModel(): BaseViewModel {
        return viewModel
    }

    override fun onCreateView(view: View) {
        subscriptionManager = SubscriptionManager(requireContext(), this)
        initOnClick()
        initAdapter()
        getSubsPlanList()

        startBillingClientConnection()
    }

    private fun initAdapter() {

        subsAdapter = SubscriptionsAdapter(requireContext(), subsList, this)
        binding.rvSubs.adapter = subsAdapter
    }


    private fun initOnClick() {
        viewModel.onClick.observe(viewLifecycleOwner, Observer { it ->
            when (it?.id) {
                R.id.ivCancel -> {
                    val didPop = findNavController().popBackStack()
                    if (!didPop) {
                        requireActivity().finish()
                    }else{
                        onBackPressed()
                    }
                }

                /* R.id.tvGoPro -> {
                     showToast("Work in progress")
                     val billingFlowParams =
                         BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build()
                     mBillingClient.launchBillingFlow(
                         requireActivity(), billingFlowParams
                     )
                 }*/

                /*R.id.cons1months -> {
                    handleSelection(
                        selectedContainer = binding.cons1months,
                        selectedTitle = binding.tv1Months,
                        selectedSubtitle = binding.tvPerMonths,
                        selectedValue = binding.ProValue1Months,
                        goProText = binding.ProValue1Months.text.toString().trim()
                    )
                }

                R.id.cons6months -> {
                    handleSelection(
                        selectedContainer = binding.cons6months,
                        selectedTitle = binding.tv2Months,
                        selectedSubtitle = binding.tvPer2Months,
                        selectedValue = binding.ProValue2Months,
                        goProText = binding.ProValue2Months.text.toString().trim()
                    )
                }

                R.id.cons12months -> {
                    handleSelection(
                        selectedContainer = binding.cons12months,
                        selectedTitle = binding.tv12Months,
                        selectedSubtitle = binding.tvPer12Months,
                        selectedValue = binding.ProValue12Months,
                        goProText = binding.ProValue12Months.text.toString().trim()
                    )
                }*/
            }
        })
    }

    private fun startBillingClientConnection() {

        subscriptionManager.startConnection {
            // Query subscriptions once connected
            subscriptionManager.queryAvailableSubscriptions { productDetailsList ->
                // Display subscription options to the user
                /*for (productDetails in productDetailsList) {
                    // Example: Show productDetails in UI
                    Log.d("productDetails", "startBillingClientConnection: ${productDetails.name}")
                }*/
                //plansList.clear()

                CoroutineScope(Dispatchers.Main).launch {
                    val job = launch {

                        productDetailsList.forEachIndexed { _, productDetails ->
                            Log.d(
                                "productDetails",
                                "startBillingClientConnection: ${productDetails.name} ,  ${productDetails.productId} , ${
                                    productDetails.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(
                                        0
                                    )?.formattedPrice
                                }"
                            )
                            val price =
                                productDetails.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(
                                    0
                                )?.formattedPrice


                           subsList.add(SubsModel(  productDetails.productId,  productDetails.title, price, "1 months", false))

                           /* subsAdapter.item = subsList
                            subsAdapter.notifyDataSetChanged()*/

                        }
                    }
                    job.join()
                    val job1 = GlobalScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main) {
                            subsAdapter.item = subsList
                            subsAdapter.notifyDataSetChanged()
                        }
                    }
                    job1.join()

                }
                Log.d("productDetails", "startBillingClientConnection: ")

                /*plansAdapter.plans = plansList
                plansAdapter.notifyDataSetChanged()*/

            }
        }
        showToast("Billing failed")
        subsAdapter.item = subsList
        subsAdapter.notifyDataSetChanged()
    }

    private fun getSubsPlanList(){
        subsList.add(SubsModel("", "item1", "$1.99", "1 Month", true))
        subsList.add(SubsModel("", "item1", "$11.99", "1 Year", false))
        // subsList.add(SubsModel("", "item1", "$3.99", "1 month", false))

        binding.tvGoPro.text = "${ContextCompat.getString(requireContext(), R.string.continue_)} ${subsList[0].price}"

    }

    private fun handleSelection(
        selectedContainer: View,
        selectedTitle: TextView,
        selectedSubtitle: TextView,
        selectedValue: TextView,
        goProText: String
    ) {
        // Highlight selected option
        highlightSelectedOption(selectedContainer, selectedTitle, selectedSubtitle, selectedValue)

        // Reset the other two options
        when (selectedContainer) {
            /*binding.cons1months -> {
                resetDefaultUI(
                    binding.cons6months,
                    binding.tv2Months,
                    binding.tvPer2Months,
                    binding.ProValue2Months
                )
                resetDefaultUI(
                    binding.cons12months,
                    binding.tv12Months,
                    binding.tvPer12Months,
                    binding.ProValue12Months
                )
            }

            binding.cons6months -> {
                resetDefaultUI(
                    binding.cons1months,
                    binding.tv1Months,
                    binding.tvPerMonths,
                    binding.ProValue1Months
                )
                resetDefaultUI(
                    binding.cons12months,
                    binding.tv12Months,
                    binding.tvPer12Months,
                    binding.ProValue12Months
                )
            }

            binding.cons12months -> {
                resetDefaultUI(
                    binding.cons1months,
                    binding.tv1Months,
                    binding.tvPerMonths,
                    binding.ProValue1Months
                )
                resetDefaultUI(
                    binding.cons6months,
                    binding.tv2Months,
                    binding.tvPer2Months,
                    binding.ProValue2Months
                )
            }*/
        }

        binding.tvGoPro.text = "CONTINUE $goProText"
    }

    private fun highlightSelectedOption(
        container: View, title: TextView, subtitle: TextView, value: TextView
    ) {
        container.backgroundTintList = null
        title.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.white))
        subtitle.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.white))
        subtitle.alpha = 0.8f
        value.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.colorAccent))
        container.setBackgroundResource(R.drawable.bg_pro_rounded)
    }

    private fun resetDefaultUI(
        container: View, title: TextView, subtitle: TextView, value: TextView
    ) {
        container.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.bg_pro)
        title.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.rating))
        subtitle.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.rating))
        value.setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.rating))
        container.setBackgroundResource(R.drawable.corner_radius_10)
    }



    override fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
           /* val plansList = plansList.find { it.id == selectedPlanId }
            val purchaseDetails = plansList?.productDetails
            val pricing = purchaseDetails?.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)*/
            // val subscriptionOfferDetails = purchase.subscriptionOfferDetails?.firstOrNull()
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            subscriptionManager.billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {

                    val hashMap = HashMap<String, Any>()
                    hashMap["purchase_id"] = purchase.orderId.toString()
                    /*hashMap["currency_code"] = pricing?.priceCurrencyCode.toString()
                    hashMap["currency_symbol"] = pricing?.priceCurrencyCode.toString()
                    hashMap["subscription_amount"] = pricing?.formattedPrice.toString()
                    hashMap["expiry_date"] = plansList?.duration.toString()
                    hashMap["subscription_category_id"] = plansList?.category_id.toString()*/

                    /*if (sharedPrefManager.getCurrentUser() != null) {
                        val dataShared = sharedPrefManager.getCurrentUser()
                        dataShared?.data?.subscriptionInfo?.purchaseSubscriptionCategoryId =
                            plansList?.category_id.toString()
                        sharedPrefManager.saveUser(dataShared!!)

                    }*/

                   // viewModel.updateSubscriptionApi(Constants.UPDATE_SUBSCRIPTION_PLAN, hashMap)

                }
            }


        }

    }

    override fun onItemCLick(model: SubsModel) {
        subsAdapter.notifyDataSetChanged()

        binding.tvGoPro.text = "${ContextCompat.getString(requireContext(), R.string.continue_)} ${model.price}"
    }

}