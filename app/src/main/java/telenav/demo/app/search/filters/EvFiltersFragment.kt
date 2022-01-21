package telenav.demo.app.search.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.constraintlayout.widget.ConstraintSet
import telenav.demo.app.databinding.EvFiltersFragmentLayoutBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.widgets.RoundedBottomSheetLayout
import java.util.ArrayList
import android.widget.RadioButton

class EvFiltersFragment : RoundedBottomSheetLayout(), View.OnClickListener {

    private var openNowFilter = OpenNowFilter()
    private val starsFilter = StarsFilter()
    private var priceLevelFilter = PriceLevel()
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
        connectionTypesArrayList.add("Type1")
        connectionTypesArrayList.add("Type2")
        connectionTypesArrayList.add("Type3")
        connectionTypesArrayList.add("Type4")
        connectionTypesArrayList.add("Type5")
        connectionTypesArrayList.add("Type6")
        connectionTypesArrayList.add("Type7")
        connectionTypesArrayList.add("Type8")
        connectionTypesArrayList.add("Type9")
        connectionTypesArrayList.add("Type10")

        val powerFeedLevelsArrayList: ArrayList<String> = ArrayList()
        powerFeedLevelsArrayList.add("Type1")
        powerFeedLevelsArrayList.add("Type2")
        powerFeedLevelsArrayList.add("Type3")

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

            binding?.evFiltersRoot?.addView(checkBox)
            evConnectionTypesIdArray.add(checkBox.id)
        }
        binding?.flowConnectorTypes?.referencedIds = evConnectionTypesIdArray.toIntArray()

        val powerFeedLevelsIdArray = ArrayList<Int>()
        for (item in powerFeedLevelsArrayList) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = item
            checkBox.id = View.generateViewId()

            binding?.evFiltersRoot?.addView(checkBox)
            powerFeedLevelsIdArray.add(checkBox.id)
        }
        binding?.flowPowerFeedLevels?.referencedIds = powerFeedLevelsIdArray.toIntArray()

        val chargerBrandsIdArray = ArrayList<Int>()
        for (item in chargerBrandsArrayList) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = item
            checkBox.id = View.generateViewId()

            binding?.evFiltersRoot?.addView(checkBox)
            chargerBrandsIdArray.add(checkBox.id)
        }
        binding?.flowChargerBrands?.referencedIds = chargerBrandsIdArray.toIntArray()
    }

    private fun setUpCLickListeners() {
        binding?.evFiltersAreaBack?.setOnClickListener {
            dismiss()
            (activity as MapActivity).setFilters(getFilters())
            (activity!! as MapActivity).onBackSearchInfoFragment()
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