package telenav.demo.app.search.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.databinding.ParkingFiltersFragmentLayoutBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.widgets.RoundedBottomSheetLayout

class ParkingFiltersFragment : RoundedBottomSheetLayout(), View.OnClickListener {

    private var openNowFilter = OpenNowFilter()
    private var priceLevelFilter = PriceLevel()
    private var reservationFilter = ReservationFilter()
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
            (activity as MapActivity).onBackSearchInfoFragmentFromFilter(getFilters())
        }
        binding?.priceReset?.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.price_level_one_dollar -> {
                initPriceLevelValue(PriceLevelType.ONE_DOLLAR.priceLevel)
            }
            R.id.price_level_two_dollars -> {
                initPriceLevelValue(PriceLevelType.TWO_DOLLAR.priceLevel)
            }
            R.id.price_level_three_dollars -> {
                initPriceLevelValue(PriceLevelType.THREE_DOLLAR.priceLevel)
            }
            R.id.price_level_four_dollars -> {
                initPriceLevelValue(PriceLevelType.FOUR_DOLLAR.priceLevel)
            }
            R.id.price_reset -> {
                initPriceLevelValue(PriceLevelType.DEFAULT.priceLevel)
            }
        }
    }

    private fun getFilters(): List<Filter> {
        val filtersToApply = arrayListOf<Filter>()
        if (openNowFilter.isOpened != OpenNow.DEFAULT) filtersToApply.add(openNowFilter)
        if (priceLevelFilter.priceLevel != PriceLevelType.DEFAULT) filtersToApply.add(
            priceLevelFilter
        )
        if (reservationFilter.isReserved != Reservation.DEFAULT) filtersToApply.add(reservationFilter)
        return filtersToApply
    }

    private fun initDefaultValues() {
        binding?.openNow?.isChecked = App.readBooleanFromSharedPreferences(App.OPEN_TIME, false)
        binding?.openNow?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                openNowFilter.isOpened = OpenNow.OPEN
                App.writeBooleanToSharedPreferences(App.OPEN_TIME, true)
            } else {
                openNowFilter.isOpened = OpenNow.DEFAULT
                App.writeBooleanToSharedPreferences(App.OPEN_TIME, false)
            }
        }

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

        initPriceLevelValue(App.readIntFromSharedPreferences(App.PRICE_LEVEL, PriceLevelType.DEFAULT.ordinal))
    }


    private fun initPriceLevelValue(priceLevel: Int) {
        when (priceLevel) {
            -1 -> {
                priceLevelFilter.priceLevel = PriceLevelType.DEFAULT
                binding?.priceLevelOneDollar?.isChecked = false
                binding?.priceLevelTwoDollars?.isChecked = false
                binding?.priceLevelThreeDollars?.isChecked = false
                binding?.priceLevelFourDollars?.isChecked = false
            }
            1 -> {
                priceLevelFilter.priceLevel = PriceLevelType.ONE_DOLLAR
                binding?.priceLevelOneDollar?.isChecked = true
                binding?.priceLevelTwoDollars?.isChecked = false
                binding?.priceLevelThreeDollars?.isChecked = false
                binding?.priceLevelFourDollars?.isChecked = false
            }
            2 -> {
                priceLevelFilter.priceLevel = PriceLevelType.TWO_DOLLAR
                binding?.priceLevelOneDollar?.isChecked = false
                binding?.priceLevelTwoDollars?.isChecked = true
                binding?.priceLevelThreeDollars?.isChecked = false
                binding?.priceLevelFourDollars?.isChecked = false
            }
            3 -> {
                priceLevelFilter.priceLevel = PriceLevelType.THREE_DOLLAR
                binding?.priceLevelOneDollar?.isChecked = false
                binding?.priceLevelTwoDollars?.isChecked = false
                binding?.priceLevelThreeDollars?.isChecked = true
                binding?.priceLevelFourDollars?.isChecked = false
            }
            4 -> {
                priceLevelFilter.priceLevel = PriceLevelType.FOUR_DOLLAR
                binding?.priceLevelOneDollar?.isChecked = false
                binding?.priceLevelTwoDollars?.isChecked = false
                binding?.priceLevelThreeDollars?.isChecked = false
                binding?.priceLevelFourDollars?.isChecked = true
            }
        }
        App.writeToSharedPreferences(App.PRICE_LEVEL, priceLevel)
    }

    companion object {
        @JvmStatic
        fun newInstance() = ParkingFiltersFragment()
    }
}