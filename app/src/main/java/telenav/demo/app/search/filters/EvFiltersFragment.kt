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
import telenav.demo.app.R

class EvFiltersFragment : RoundedBottomSheetLayout(), View.OnClickListener {

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
        connectionTypesArrayList.add("Type 2")
        connectionTypesArrayList.add("Type 3")
        connectionTypesArrayList.add("Teala")
        connectionTypesArrayList.add("NEMA")
        connectionTypesArrayList.add("NEMA 14-50")
        connectionTypesArrayList.add("Plug Type F")

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

        val evConnectionTypesIdArray = ArrayList<Int>()
        for (item in connectionTypesArrayList) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = item
            checkBox.id = View.generateViewId()
            checkBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.black_c1))
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

            binding?.evFiltersRoot?.addView(checkBox)
            evConnectionTypesIdArray.add(checkBox.id)
        }
        binding?.flowConnectorTypes?.referencedIds = evConnectionTypesIdArray.toIntArray()

        val powerFeedLevelsIdArray = ArrayList<Int>()
        for (item in powerFeedLevelsArrayList) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = item
            checkBox.id = View.generateViewId()
            checkBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.black_c1))
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

            binding?.evFiltersRoot?.addView(checkBox)
            powerFeedLevelsIdArray.add(checkBox.id)
        }
        binding?.flowPowerFeedLevels?.referencedIds = powerFeedLevelsIdArray.toIntArray()

        val chargerBrandsIdArray = ArrayList<Int>()
        for (item in chargerBrandsArrayList) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = item
            checkBox.id = View.generateViewId()
            checkBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.black_c1))
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

            binding?.evFiltersRoot?.addView(checkBox)
            chargerBrandsIdArray.add(checkBox.id)
        }
        binding?.flowChargerBrands?.referencedIds = chargerBrandsIdArray.toIntArray()

        binding?.reservation?.isChecked = false
        binding?.reservation?.setOnClickListener {
            if (it.isEnabled) {
                reservationFilter.isReserved = Reservation.RESERVED
            } else {
                reservationFilter.isReserved = Reservation.DEFAULT
            }
        }
    }

    private fun setUpCLickListeners() {
        binding?.evFiltersAreaBack?.setOnClickListener {
            dismiss()
            (activity!! as MapActivity).onBackSearchInfoFragmentFromFilter(getFilters())
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
        }
    }

    private fun getFilters(): List<Filter> {
        val filtersToApply = arrayListOf<Filter>()
        if (openNowFilter.isOpened != OpenNow.DEFAULT) filtersToApply.add(openNowFilter)
        if (priceLevelFilter.priceLevel != PriceLevelType.DEFAULT) filtersToApply.add(
            priceLevelFilter
        )
        if (starsFilter.stars != Stars.DEFAULT) filtersToApply.add(starsFilter)
        return filtersToApply
    }

    companion object {
        @JvmStatic
        fun newInstance() = EvFiltersFragment()
    }
}