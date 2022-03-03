package telenav.demo.app.search.filters

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.constraintlayout.widget.ConstraintSet
import telenav.demo.app.databinding.EvFiltersFragmentLayoutBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.widgets.RoundedBottomSheetLayout
import java.util.ArrayList
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import telenav.demo.app.App
import telenav.demo.app.R

class EvFiltersFragment : RoundedBottomSheetLayout() , View.OnClickListener {

    private var openNowFilter = OpenNowFilter()
    private val starsFilter = StarsFilter()
    private var priceLevelFilter = PriceLevel()
    private var reservationFilter = ReservationFilter()
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

        val connectionTypesArrayList: ArrayList<String> = ArrayList()
        connectionTypesArrayList.add("J1772")
        connectionTypesArrayList.add("Sae Combo")
        connectionTypesArrayList.add("CHAdeMo")
        connectionTypesArrayList.add("NEMA")
        connectionTypesArrayList.add("NEMA 14-50")
        connectionTypesArrayList.add("Plug Type F")
        connectionTypesArrayList.add("Type 2")
        connectionTypesArrayList.add("Type 3")
        connectionTypesArrayList.add("Teala")

        val powerFeedLevelsArrayList: ArrayList<String> = ArrayList()
        powerFeedLevelsArrayList.add("Level 1")
        powerFeedLevelsArrayList.add("Level 2")
        powerFeedLevelsArrayList.add("DC Fast")

        val chargerBrandsArrayList: ArrayList<String> = ArrayList()
        chargerBrandsArrayList.add("Type1")
        chargerBrandsArrayList.add("Type2")
        chargerBrandsArrayList.add("Type3")

        val set = ConstraintSet()
        set.clone(binding?.evFiltersRoot)

        val connectionTypes: String? = App.readStringFromSharedPreferences(App.CONNECTION_TYPES, "")
        val connectionTypesLstValues: List<String>? = connectionTypes?.split(",")?.map { it -> it.trim() }
        val evConnectionTypesIdArray = ArrayList<Int>()

        for (item in connectionTypesArrayList) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = item
            checkBox.id = View.generateViewId()
            checkBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.black_c1))
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

            connectionTypesLstValues?.forEach  {
                if (item.equals(it)) {
                    checkBox.isChecked = true
                }
            }

            binding?.evFiltersRoot?.addView(checkBox)
            evConnectionTypesIdArray.add(checkBox.id)
        }
        binding?.flowConnectorTypes?.referencedIds = evConnectionTypesIdArray.toIntArray()

        val powerFeedLevels: String? = App.readStringFromSharedPreferences(App.POWER_FEED, "")
        val lstPowerFeedLevelsValues: List<String>? = powerFeedLevels?.split(",")?.map { it -> it.trim() }
        val powerFeedLevelsIdArray = ArrayList<Int>()

        for (item in powerFeedLevelsArrayList) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = item
            checkBox.id = View.generateViewId()
            checkBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.black_c1))
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

            lstPowerFeedLevelsValues?.forEach  {
                if (item.equals(it)) {
                    checkBox.isChecked = true
                }
            }

            binding?.evFiltersRoot?.addView(checkBox)
            powerFeedLevelsIdArray.add(checkBox.id)
        }
        binding?.flowPowerFeedLevels?.referencedIds = powerFeedLevelsIdArray.toIntArray()

        val chargerBrandsLevels: String? = App.readStringFromSharedPreferences(App.CHARGER_BRAND, "")
        val lstChargerBrandsValues: List<String>? = chargerBrandsLevels?.split(",")?.map { it -> it.trim() }
        val chargerBrandsIdArray = ArrayList<Int>()

        for (item in chargerBrandsArrayList) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = item
            checkBox.id = View.generateViewId()
            checkBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.black_c1))
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

            lstChargerBrandsValues?.forEach  {
                if (item.equals(it)) {
                    checkBox.isChecked = true
                }
            }

            binding?.evFiltersRoot?.addView(checkBox)
            chargerBrandsIdArray.add(checkBox.id)
        }
        binding?.flowChargerBrands?.referencedIds = chargerBrandsIdArray.toIntArray()

        binding?.reservation?.isChecked = App.readBooleanFromSharedPreferences(App.RESERVED, false)
        binding?.reservation?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                reservationFilter.isReserved = Reservation.RESERVED
                App.writeBooleanToSharedPreferences(App.RESERVED, true)
            } else {
                reservationFilter.isReserved = Reservation.DEFAULT
                App.writeBooleanToSharedPreferences(App.RESERVED, false)
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ev_connector_types_reset -> {
                App.writeStringToSharedPreferences(App.CONNECTION_TYPES, "")
                binding?.evFiltersRoot?.forEach {
                    if (it is CheckBox) {
                        if (binding?.flowConnectorTypes?.referencedIds?.contains(it.id) == true) {
                            it.isChecked = false
                        }
                    }
                }
            }
            R.id.ev_power_feed_levels_reset -> {
                App.writeStringToSharedPreferences(App.POWER_FEED, "")
                binding?.evFiltersRoot?.forEach {
                    if (it is CheckBox) {
                        if (binding?.flowPowerFeedLevels?.referencedIds?.contains(it.id) == true) {
                            it.isChecked = false
                        }
                    }
                }
            }
            R.id.ev_charger_brands_reset -> {
                App.writeStringToSharedPreferences(App.CHARGER_BRAND, "")
                binding?.evFiltersRoot?.forEach {
                    if (it is CheckBox) {
                        if (binding?.flowChargerBrands?.referencedIds?.contains(it.id) == true) {
                            it.isChecked = false
                        }
                    }
                }
            }
        }
    }

    private fun setUpCLickListeners() {
        binding?.evFiltersAreaBack?.setOnClickListener {
            dismiss()
            (activity!! as MapActivity).onBackSearchInfoFragmentFromFilter(getFilters())
        }
        binding?.evConnectorTypesReset?.setOnClickListener(this)
        binding?.evPowerFeedLevelsReset?.setOnClickListener(this)
        binding?.evChargerBrandsReset?.setOnClickListener(this)
    }

    private fun getFilters(): List<Filter> {
        val filtersToApply = arrayListOf<Filter>()
        if (openNowFilter.isOpened != OpenNow.DEFAULT) filtersToApply.add(openNowFilter)
        if (priceLevelFilter.priceLevel != PriceLevelType.DEFAULT) filtersToApply.add(
            priceLevelFilter
        )
        if (starsFilter.stars != Stars.DEFAULT) filtersToApply.add(starsFilter)

        var connectionTypes = ""
        var chargerBrands = ""
        var powerFeed = ""
        binding?.evFiltersRoot?.forEach {
            if (it is CheckBox && it.isChecked) {
                when {
                    binding?.flowConnectorTypes?.referencedIds?.contains(it.id) == true -> {
                        connectionTypes = connectionTypes + it.text + ", "
                    }
                    binding?.flowChargerBrands?.referencedIds?.contains(it.id) == true -> {
                        chargerBrands = chargerBrands + it.text + ", "
                    }
                    binding?.flowPowerFeedLevels?.referencedIds?.contains(it.id) == true -> {
                        powerFeed = powerFeed + it.text + ","
                    }
                }
            }
        }
        App.writeStringToSharedPreferences(App.CONNECTION_TYPES, connectionTypes)
        App.writeStringToSharedPreferences(App.CHARGER_BRAND, chargerBrands)
        App.writeStringToSharedPreferences(App.POWER_FEED, powerFeed)

        return filtersToApply
    }

    companion object {
        @JvmStatic
        fun newInstance() = EvFiltersFragment()
    }
}