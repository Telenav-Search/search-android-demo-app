package telenav.demo.app.search.filters

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.*

import com.github.gzuliyujiang.wheelpicker.contract.DateFormatter
import com.github.gzuliyujiang.wheelpicker.entity.DatimeEntity
import com.github.gzuliyujiang.wheelpicker.widget.DatimeWheelLayout

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import telenav.demo.app.App
import telenav.demo.app.R
import telenav.demo.app.databinding.ParkingFiltersFragmentLayoutBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.utils.Converter
import telenav.demo.app.widgets.OutsideEventLayout

import java.text.SimpleDateFormat
import java.util.*

class ParkingFiltersFragment : BottomSheetDialogFragment(),
    View.OnClickListener, OutsideEventLayout.OnTouchOutsideListener {

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

        // init DateTimePicker
        val picker = binding!!.dateTimePicker
        picker.setSelectedTextColor(requireActivity().getColor(R.color.speech_ready_neutral))
        picker.setDateFormatter(CustomDateFormatter())
        (binding!!.root as OutsideEventLayout).setListener(binding!!.dateTimeContainer, this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.let { (it.root as OutsideEventLayout).removeListener(it.dateTimeContainer) }
    }

    private fun setUpCLickListeners() {
        val fragment = this
        binding?.apply {
            priceLevelOneDollar.setOnClickListener(fragment)
            priceLevelTwoDollars.setOnClickListener(fragment)
            priceLevelThreeDollars.setOnClickListener(fragment)
            priceLevelFourDollars.setOnClickListener(fragment)
            parkingFiltersAreaBack.setOnClickListener { onBack() }
            priceReset.setOnClickListener(fragment)
            parkingDurationAdd.setOnClickListener(fragment)
            parkingDurationSubstract.setOnClickListener(fragment)
            parkingTimeStartFromReset.setOnClickListener(fragment)
            parkingDurationReset.setOnClickListener(fragment)
            dateTimeOverlay.setOnClickListener { changePickerState(false) }
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
            R.id.parking_time_start_from_reset-> {
                binding?.dateTimePicker?.apply {
                    setDefaultValue(DatimeEntity.now())
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

    private fun onBack() {
        App.writeToSharedPreferences(App.PARKING_DURATION, parkingDuration)
        App.writeStringToSharedPreferences(App.PARKING_START_FROM, getParkingStartFromDate())
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
        initParkingDuration()
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
        binding?.parkingDuration?.text = "$parkingDuration : 00 HR"
    }

    private fun getParkingStartFromDate(): String {
        // get date and time from widget
        val dateTime = binding?.dateTimePicker ?: return ""
        val month = dateTime.selectedMonth
        val date = dateTime.selectedDay
        val hour = dateTime.selectedHour
        val min = dateTime.selectedMinute

        // use calendar and timezone to format date and time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1) // Calendar's Month starts with 0 (January)
            set(Calendar.DAY_OF_MONTH, date)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, min)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return (sdf.format(calendar.time))
    }

    class TextColorTransition(private val targetColor: Int): Transition() {
        private val PROPNAME_TEXTCOLOR = "telenav:TextColorTransition:text_color"

        override fun captureStartValues(transitionValues: TransitionValues) {
            captureValues(transitionValues)
        }

        override fun captureEndValues(transitionValues: TransitionValues) {
            transitionValues.values[PROPNAME_TEXTCOLOR] = targetColor
        }

        private fun captureValues(transitionValues: TransitionValues) {
            val view = transitionValues.view as? DatimeWheelLayout ?: return
            transitionValues.values[PROPNAME_TEXTCOLOR] = view.monthWheelView.selectedTextColor // get color from wheel
        }

        override fun createAnimator(
            sceneRoot: ViewGroup,
            startValues: TransitionValues?,
            endValues: TransitionValues?
        ): Animator? {
            startValues ?: return null
            endValues ?: return null
            val view = startValues.view as? DatimeWheelLayout ?: return null
            val startColor = startValues.values[PROPNAME_TEXTCOLOR] as Int
            val endColor = endValues.values[PROPNAME_TEXTCOLOR] as Int
            val animator = ValueAnimator.ofArgb(startColor, endColor)
            animator.addUpdateListener {
                val value = it.animatedValue
                if (value is Int) {
                    view.setSelectedTextColor(value)
                }
            }
            return animator
        }
    }

    private fun changePickerState(shouldCollapse: Boolean) {
        // show/hide overlay
        binding?.dateTimeOverlay?.visibility = if (shouldCollapse) View.VISIBLE else View.GONE

        // TextColorTransition
        val targetColor = if (shouldCollapse) {
            requireContext().getColor(R.color.speech_ready_neutral)
        } else {
            requireContext().getColor(R.color.blue_c1)
        }
        val colorTransition = TextColorTransition(targetColor).addTarget(binding!!.dateTimePicker)
        val container = binding!!.dateTimeContainer

        // ------ Transition starts from here ------
        TransitionManager.beginDelayedTransition(
            binding!!.root.parent as ViewGroup, TransitionSet().apply {
                addTransition(ChangeBounds())
                addTransition(colorTransition)
                duration = TRANSITION_DURATION
                interpolator = FastOutSlowInInterpolator()
            }
        )

        // ChangeBounds
        val param = container.layoutParams
        param.height = if (shouldCollapse)
            Converter.convertDpToPixel(requireContext(), 35f) else ViewGroup.LayoutParams.WRAP_CONTENT
        container.layoutParams = param
    }

    override fun onTouchOutside(view: View) {
        if (view === binding?.dateTimeContainer) {
            changePickerState(true)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ParkingFiltersFragment()
        private const val TRANSITION_DURATION = 300L
    }

    class CustomDateFormatter: DateFormatter {
        private val map: Map<Int, String> = mapOf(
            1 to "Jan",
            2 to "Feb",
            3 to "Mar",
            4 to "Apr",
            5 to "May",
            6 to "Jun",
            7 to "Jul",
            8 to "Aug",
            9 to "Sept",
            10 to "Oct",
            11 to "Nov",
            12 to "Dec"
        )

        override fun formatYear(year: Int): String {
            return ""
        }

        override fun formatMonth(month: Int): String {
            return map[month] ?: ""
        }

        override fun formatDay(day: Int): String {
            return day.toString()
        }
    }
}