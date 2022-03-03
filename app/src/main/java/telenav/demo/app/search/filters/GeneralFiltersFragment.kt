package telenav.demo.app.search.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.databinding.GeneralFiltersFragmentLayoutBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.widgets.RoundedBottomSheetLayout

class GeneralFiltersFragment : RoundedBottomSheetLayout(), View.OnClickListener {

    private var openNowFilter = OpenNowFilter()
    private val starsFilter = StarsFilter()
    private var priceLevelFilter = PriceLevel()
    private var binding: GeneralFiltersFragmentLayoutBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = GeneralFiltersFragmentLayoutBinding.inflate(inflater, container, false)
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
        binding?.star0?.setOnClickListener(this)
        binding?.star1?.setOnClickListener(this)
        binding?.star2?.setOnClickListener(this)
        binding?.star3?.setOnClickListener(this)
        binding?.star4?.setOnClickListener(this)
        binding?.ratingReset?.setOnClickListener(this)
        binding?.priceReset?.setOnClickListener(this)
        binding?.parkingFiltersAreaBack?.setOnClickListener {
            dismiss()
            (activity!! as MapActivity).onBackSearchInfoFragmentFromFilter(getFilters())
        }
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
            R.id.star_0 -> {
                initRateValue(1)
            }
            R.id.star_1 -> {
                initRateValue(2)
            }
            R.id.star_2 -> {
                initRateValue(3)
            }
            R.id.star_3 -> {
                initRateValue(4)
            }
            R.id.star_4 -> {
                initRateValue(5)
            }
            R.id.rating_reset -> {
                initRateValue(Stars.DEFAULT.starsNumber)
            }
            R.id.price_reset -> {
                initPriceLevelValue(PriceLevelType.DEFAULT.priceLevel)
            }
        }
    }

    private fun getFilters(): List<Filter> {
        val filtersToApply = arrayListOf<Filter>()
        if (openNowFilter.isOpened != OpenNow.DEFAULT) filtersToApply.add(openNowFilter)
        if (priceLevelFilter.priceLevel != PriceLevelType.DEFAULT)
            filtersToApply.add(priceLevelFilter
        )
        if (starsFilter.stars != Stars.DEFAULT) filtersToApply.add(starsFilter)
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

        initRateValue(App.readIntFromSharedPreferences(App.RATE_STARS, Stars.DEFAULT.starsNumber))
        initPriceLevelValue(App.readIntFromSharedPreferences(App.PRICE_LEVEL, PriceLevelType.DEFAULT.priceLevel))
    }

    private fun initRateValue(rate: Int) {
        when (rate) {
            -1 -> {
                starsFilter.stars = Stars.DEFAULT
                binding?.starCount?.text = ""
            }
            1 -> {
                starsFilter.stars = Stars.ONE
                binding?.starCount?.text = "1.0"
            }
            2 -> {
                starsFilter.stars = Stars.TWO
                binding?.starCount?.text = "2.0"
            }
            3 -> {
                starsFilter.stars = Stars.TREE
                binding?.starCount?.text = "3.0"
            }
            4 -> {
                starsFilter.stars = Stars.FOUR
                binding?.starCount?.text = "4.0"
            }
            5 -> {
                starsFilter.stars = Stars.FIVE
                binding?.starCount?.text = "5.0"
            }
        }
        App.writeToSharedPreferences(App.RATE_STARS, rate)
        CategoryAndFiltersUtil.setStarsViewBasedOnRating(
            view!!,
            starsFilter.stars.starsNumber.toDouble(),
            requireContext()
        )
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
        fun newInstance() = GeneralFiltersFragment()
    }
}