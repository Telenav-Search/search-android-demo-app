package telenav.demo.app.search.filters

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.filters_fragment_layout.*
import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.databinding.FiltersFragmentLayoutBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.utils.CategoryAndFiltersUtil
import telenav.demo.app.utils.CategoryAndFiltersUtil.setStarsViewBasedOnRating
import telenav.demo.app.widgets.RoundedBottomSheetLayout

class FiltersFragment : RoundedBottomSheetLayout(), View.OnClickListener {

    private val viewModel: FiltersViewModel by viewModels()

    private var openNowFilter = OpenNowFilter()
    private val starsFilter = StarsFilter()
    private var priceLevelFilter = PriceLevel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = FiltersFragmentLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDefaultValues()
        setUpCLickListeners()
    }

    private fun setUpCLickListeners() {
        apply_filters.setOnClickListener(this)
        star_0_rb.setOnClickListener(this)
        star_1_rb.setOnClickListener(this)
        star_2_rb.setOnClickListener(this)
        star_3_rb.setOnClickListener(this)
        star_4_rb.setOnClickListener(this)
        price_level_one_dollar.setOnClickListener(this)
        price_level_two_dollars.setOnClickListener(this)
        price_level_three_dollars.setOnClickListener(this)
        price_level_four_dollars.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.apply_filters -> {
                val number = nr_of_results_et.text.toString()
                if (!number.isEmpty()) {
                    App.writeToSharedPreferences(
                        App.FILTER_NUMBER,
                        Integer.parseInt(number.toString())
                    )
                }
                (activity as MapActivity).setFilters(getFilters())
                dismiss()
            }

            R.id.star_0_rb -> {
                starsFilter.stars = Stars.ONE
                setStarsViewBasedOnRating(
                    view!!,
                    Stars.ONE.starsNumber.toDouble(),
                    requireContext()
                )

            }
            R.id.star_1_rb -> {
                starsFilter.stars = Stars.TWO
                setStarsViewBasedOnRating(
                    view!!,
                    Stars.TWO.starsNumber.toDouble(),
                    requireContext()
                )
            }
            R.id.star_2_rb -> {
                starsFilter.stars = Stars.TREE
                setStarsViewBasedOnRating(
                    view!!,
                    Stars.TREE.starsNumber.toDouble(),
                    requireContext()
                )
            }
            R.id.star_3_rb -> {
                starsFilter.stars = Stars.FOUR
                setStarsViewBasedOnRating(
                    view!!,
                    Stars.FOUR.starsNumber.toDouble(),
                    requireContext()
                )
            }
            R.id.star_4_rb -> {
                starsFilter.stars = Stars.FIVE
                setStarsViewBasedOnRating(
                    view!!,
                    Stars.FIVE.starsNumber.toDouble(),
                    requireContext()
                )
            }

            //click on price levels
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
        open_now.isChecked = false
        open_now.setOnClickListener {
            if (it.isEnabled) {
                openNowFilter.isOpened = OpenNow.OPEN
            } else {
                openNowFilter.isOpened = OpenNow.DEFAULT
            }
        }

        initNrOfResults()
        initChips()
    }

    private fun initNrOfResults() {
        nr_of_results_et.setText(App.readFromSharedPreferences(App.FILTER_NUMBER).toString())
    }

    // init info on the screen: labels on chips, search results number
    private fun initChips() {
        CategoryAndFiltersUtil.searchLabelsList.forEach {
            addCategoryChip(it)
        }
    }

    /**
     * add label preferences on the screen as chips
     */
    private fun addCategoryChip(label: String) {
        val chip = Chip(context)
        chip.text = label
        chip.chipBackgroundColor =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    CategoryAndFiltersUtil.categoriesColors.random()
                )
            )
        chip.isCheckable = true
        chip.isCloseIconVisible = false
        labels_chip_group.addView(chip as View)
    }
}