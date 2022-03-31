package telenav.demo.app.search.filters

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.databinding.ParkingFiltersFragmentLayoutBinding
import telenav.demo.app.map.MapActivity

class ParkingFiltersFragment : BottomSheetDialogFragment(), View.OnClickListener {

    private var priceLevelFilter = PriceLevel()
    private var binding: ParkingFiltersFragmentLayoutBinding? = null
    private var parkingDuration = 0
    private var parkingStartDuration = 0

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
            onBack()
        }
        binding?.priceReset?.setOnClickListener(this)
        binding?.parkingDurationAdd?.setOnClickListener(this)
        binding?.parkingDurationSubstract?.setOnClickListener(this)
        binding?.parkingTimeStartFromReset?.setOnClickListener(this)
        binding?.parkingTimeStartAdd?.setOnClickListener(this)
        binding?.parkingTimeStartSubtract?.setOnClickListener(this)
        binding?.parkingDurationReset?.setOnClickListener(this)
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
            R.id.parking_duration_add -> {
                parkingDuration++
                initParkingDuration()
            }
            R.id.parking_duration_substract -> {
                if (parkingDuration != 0) {
                    parkingDuration--
                }
                initParkingDuration()
            }
            R.id.parking_duration_reset -> {
                parkingDuration = 0
                App.writeToSharedPreferences(App.PARKING_DURATION, parkingDuration)
                initParkingDuration()
            }
            R.id.parking_time_start_add -> {
                parkingStartDuration++
                initStartDuration()
            }
            R.id.parking_time_start_subtract -> {
                if (parkingStartDuration != 0) {
                    parkingStartDuration--
                }
                initStartDuration()
            }
            R.id.parking_time_start_from_reset-> {
                parkingStartDuration = 0
                App.writeToSharedPreferences(App.PARKING_START_FROM, parkingStartDuration)
                initStartDuration()
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

    private fun onBack() {
        App.writeToSharedPreferences(App.PARKING_DURATION, parkingDuration)
        App.writeToSharedPreferences(App.PARKING_START_FROM, parkingStartDuration)
        (activity as MapActivity).onBackFromFilterFragment()
        dismiss()
    }

    private fun initDefaultValues() {
        binding?.openNow?.isChecked = App.readBooleanFromSharedPreferences(App.OPEN_TIME, false)
        binding?.openNow?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                App.writeBooleanToSharedPreferences(App.OPEN_TIME, true)
            } else {
                App.writeBooleanToSharedPreferences(App.OPEN_TIME, false)
            }
        }

        binding?.reservation?.isChecked = App.readBooleanFromSharedPreferences(App.RESERVED, false)
        binding?.reservation?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                App.writeBooleanToSharedPreferences(App.RESERVED, true)
            } else {
                App.writeBooleanToSharedPreferences(App.RESERVED, false)
            }
        }

        initPriceLevelValue(App.readIntFromSharedPreferences(App.PRICE_LEVEL, PriceLevelType.DEFAULT.ordinal))
        parkingDuration = App.readIntFromSharedPreferences(App.PARKING_DURATION, 0)
        parkingStartDuration = App.readIntFromSharedPreferences(App.PARKING_START_FROM, 0)
        initParkingDuration()
        initStartDuration()
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

    private fun initParkingDuration() {
        binding?.parkingDuration?.text = "$parkingDuration : 00 MN"
    }

    private fun initStartDuration() {
        val min = parkingStartDuration / 60 % 60
        val sec = parkingStartDuration % 60
        val formatText = String.format("%02d : %02d", min, sec)
        binding?.parkingTimeStartFrom?.text = formatText
    }

    companion object {
        @JvmStatic
        fun newInstance() = ParkingFiltersFragment()
    }
}