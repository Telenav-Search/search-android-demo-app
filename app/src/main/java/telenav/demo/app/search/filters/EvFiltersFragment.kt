package telenav.demo.app.search.filters

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox

import androidx.constraintlayout.helper.widget.Flow
import androidx.core.view.forEach

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import telenav.demo.app.databinding.EvFiltersFragmentLayoutBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.utils.CategoryAndFiltersUtil

class EvFiltersFragment : BottomSheetDialogFragment() , View.OnClickListener {

    private var binding: EvFiltersFragmentLayoutBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = EvFiltersFragmentLayoutBinding.inflate(inflater, container, false)
        return binding?.root
    }

    private fun fillFlowContent(sharedPreferencesKey: String, contentList: Set<String>, parent: ViewGroup, flow: Flow) {
        val types = App.readStringFromSharedPreferences(sharedPreferencesKey, "")
        val lstValues = types?.split(",")?.map { it -> it.trim() }
        lstValues ?: return

        val ids = mutableListOf<Int>()

        for (item in contentList) {
            val checkBox = LayoutInflater.from(requireContext()).inflate(R.layout.ev_filter_checkbox,
                parent, false) as CheckBox
            checkBox.text = item
            checkBox.tag = item
            checkBox.id = View.generateViewId()

            lstValues.forEach  {
                if (item == it) {
                    checkBox.isChecked = true
                }
            }

            parent.addView(checkBox)
            ids.add(checkBox.id)
        }
        flow.referencedIds = ids.toIntArray()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpCLickListeners()

        binding ?: return
        val localBinding = binding!!

        // fill Flow layouts with contents
        fillFlowContent(App.CONNECTION_TYPES, CategoryAndFiltersUtil.connectorTypesMap.keys,
            localBinding.evConstraintLayout, localBinding.flowConnectorTypes)
        fillFlowContent(App.POWER_FEED, CategoryAndFiltersUtil.powerFeedLevelsMap.keys,
            localBinding.evConstraintLayout, localBinding.flowPowerFeedLevels)
        fillFlowContent(App.CHARGER_BRAND, CategoryAndFiltersUtil.chargerBrandsMap.keys,
            localBinding.evConstraintLayout, localBinding.flowChargerBrands)

        binding?.freeCharger?.isChecked = App.readBooleanFromSharedPreferences(App.FREE_CHARGER, false)
        binding?.freeCharger?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                App.writeBooleanToSharedPreferences(App.FREE_CHARGER, true)
            } else {
                App.writeBooleanToSharedPreferences(App.FREE_CHARGER, false)
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ev_connector_types_reset -> {
                App.writeStringToSharedPreferences(App.CONNECTION_TYPES, "")
                binding?.evConstraintLayout?.forEach {
                    if (it is CheckBox) {
                        if (binding?.flowConnectorTypes?.referencedIds?.contains(it.id) == true) {
                            it.isChecked = false
                        }
                    }
                }
            }
            R.id.ev_power_feed_levels_reset -> {
                App.writeStringToSharedPreferences(App.POWER_FEED, "")
                binding?.evConstraintLayout?.forEach {
                    if (it is CheckBox) {
                        if (binding?.flowPowerFeedLevels?.referencedIds?.contains(it.id) == true) {
                            it.isChecked = false
                        }
                    }
                }
            }
            R.id.ev_charger_brands_reset -> {
                App.writeStringToSharedPreferences(App.CHARGER_BRAND, "")
                binding?.evConstraintLayout?.forEach {
                    if (it is CheckBox) {
                        if (binding?.flowChargerBrands?.referencedIds?.contains(it.id) == true) {
                            it.isChecked = false
                        }
                    }
                }
            }
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : BottomSheetDialog(requireContext(), theme) {
            override fun onBackPressed() {
                onBack()
            }
        }.apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
        }
    }

    private fun setUpCLickListeners() {
        binding?.evFiltersAreaBack?.setOnClickListener {
            onBack()
        }
        binding?.evConnectorTypesReset?.setOnClickListener(this)
        binding?.evPowerFeedLevelsReset?.setOnClickListener(this)
        binding?.evChargerBrandsReset?.setOnClickListener(this)
    }

    private fun onBack() {
        saveFilters()
        (activity!! as MapActivity).onBackFromFilterFragment()
        dismiss()
    }

    private fun saveFilters() {
        var connectionTypes = ""
        var chargerBrands = ""
        var powerFeed = ""
        binding?.evConstraintLayout?.forEach {
            if (it is CheckBox && it.isChecked) {
                when {
                    binding?.flowConnectorTypes?.referencedIds?.contains(it.id) == true -> {
                        connectionTypes = connectionTypes + it.tag + ", "
                    }
                    binding?.flowChargerBrands?.referencedIds?.contains(it.id) == true -> {
                        chargerBrands = chargerBrands + it.tag + ", "
                    }
                    binding?.flowPowerFeedLevels?.referencedIds?.contains(it.id) == true -> {
                        powerFeed = powerFeed + it.tag + ","
                    }
                }
            }
        }
        App.writeStringToSharedPreferences(App.CONNECTION_TYPES, connectionTypes)
        App.writeStringToSharedPreferences(App.CHARGER_BRAND, chargerBrands)
        App.writeStringToSharedPreferences(App.POWER_FEED, powerFeed)
    }

    companion object {
        @JvmStatic
        fun newInstance() = EvFiltersFragment()
    }
}