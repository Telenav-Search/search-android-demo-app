package telenav.demo.app.search.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import telenav.demo.app.R
import telenav.demo.app.databinding.ParkingFiltersFragmentLayoutBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.widgets.RoundedBottomSheetLayout

class ParkingFiltersFragment : RoundedBottomSheetLayout(), View.OnClickListener {

    private var openNowFilter = OpenNowFilter()
    private val starsFilter = StarsFilter()
    private var priceLevelFilter = PriceLevel()
    private var binding: ParkingFiltersFragmentLayoutBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ParkingFiltersFragmentLayoutBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDefaultValues()
        setUpCLickListeners()
    }

    private fun setUpCLickListeners() {
        binding?.priceLevelOneDollar?.setOnClickListener(this)
        binding?.priceLevelTwoDollars?.setOnClickListener(this)
        binding?.priceLevelThreeDollars?.setOnClickListener(this)
        binding?.priceLevelFourDollars?.setOnClickListener(this)
        binding?.parkingFiltersAreaBack?.setOnClickListener {
            dismiss()
            (activity as MapActivity).setFilters(getFilters())
            (activity!! as MapActivity).onBackSearchInfoFragment()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.price_level_one_dollar -> {
                priceLevelFilter.priceLevel = PriceLevelType.ONE_DOLLAR
            }
            R.id.price_level_two_dollars -> {
                priceLevelFilter.priceLevel = PriceLevelType.TWO_DOLLAR
            }
            R.id.price_level_three_dollars -> {
                priceLevelFilter.priceLevel = PriceLevelType.THREE_DOLLAR
            }
            R.id.price_level_four_dollars -> {
                priceLevelFilter.priceLevel = PriceLevelType.FOUR_DOLLAR
            }

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

    private fun initDefaultValues() {
        binding?.openNow?.isChecked = false
        binding?.openNow?.setOnClickListener {
            if (it.isEnabled) {
                openNowFilter.isOpened = OpenNow.OPEN
            } else {
                openNowFilter.isOpened = OpenNow.DEFAULT
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ParkingFiltersFragment()
    }
}