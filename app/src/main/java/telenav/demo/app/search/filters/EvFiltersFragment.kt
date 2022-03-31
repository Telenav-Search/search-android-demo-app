package telenav.demo.app.search.filters

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.constraintlayout.widget.ConstraintSet
import telenav.demo.app.databinding.EvFiltersFragmentLayoutBinding
import telenav.demo.app.map.MapActivity
import java.util.ArrayList
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.utils.StringUtil

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpCLickListeners()

        val set = ConstraintSet()
        set.clone(binding?.evConstraintLayout)

        val connectionTypes: String? = App.readStringFromSharedPreferences(App.CONNECTION_TYPES, "")
        val connectionTypesLstValues: List<String>? = connectionTypes?.split(",")?.map { it -> it.trim() }
        val evConnectionTypesIdArray = ArrayList<Int>()

        for (item in CategoryAndFiltersUtil.connectionTypesArrayList) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = StringUtil.formatName(item)
            checkBox.tag = item
            checkBox.id = View.generateViewId()
            checkBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.black_c1))
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

            connectionTypesLstValues?.forEach  {
                if (item.equals(it)) {
                    checkBox.isChecked = true
                }
            }

            binding?.evConstraintLayout?.addView(checkBox)
            evConnectionTypesIdArray.add(checkBox.id)
        }
        binding?.flowConnectorTypes?.referencedIds = evConnectionTypesIdArray.toIntArray()

        val powerFeedLevels: String? = App.readStringFromSharedPreferences(App.POWER_FEED, "")
        val lstPowerFeedLevelsValues: List<String>? = powerFeedLevels?.split(",")?.map { it -> it.trim() }
        val powerFeedLevelsIdArray = ArrayList<Int>()

        for (item in CategoryAndFiltersUtil.powerFeedLevelsArrayList) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = StringUtil.formatName(item)
            checkBox.tag = item
            checkBox.id = View.generateViewId()
            checkBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.black_c1))
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

            lstPowerFeedLevelsValues?.forEach  {
                if (item.equals(it)) {
                    checkBox.isChecked = true
                }
            }

            binding?.evConstraintLayout?.addView(checkBox)
            powerFeedLevelsIdArray.add(checkBox.id)
        }
        binding?.flowPowerFeedLevels?.referencedIds = powerFeedLevelsIdArray.toIntArray()

        val chargerBrandsLevels: String? = App.readStringFromSharedPreferences(App.CHARGER_BRAND, "")
        val lstChargerBrandsValues: List<String>? = chargerBrandsLevels?.split(",")?.map { it -> it.trim() }
        val chargerBrandsIdArray = ArrayList<Int>()

        for (item in CategoryAndFiltersUtil.chargerBrandsArrayList) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = StringUtil.formatName(item)
            checkBox.tag = item
            checkBox.id = View.generateViewId()
            checkBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.black_c1))
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

            lstChargerBrandsValues?.forEach  {
                if (item.equals(it)) {
                    checkBox.isChecked = true
                }
            }

            binding?.evConstraintLayout?.addView(checkBox)
            chargerBrandsIdArray.add(checkBox.id)
        }
        binding?.flowChargerBrands?.referencedIds = chargerBrandsIdArray.toIntArray()

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