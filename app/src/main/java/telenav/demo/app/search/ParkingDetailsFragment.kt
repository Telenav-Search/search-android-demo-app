package telenav.demo.app.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.telenav.sdk.entity.model.base.ParkingPriceItem
import telenav.demo.app.R
import telenav.demo.app.databinding.FragmentParkingDetailsBinding
import telenav.demo.app.model.SearchResult
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import com.telenav.sdk.entity.model.base.DataField
import com.telenav.sdk.entity.model.base.Entity
import telenav.demo.app.utils.Converter.convertDpToPixel

class ParkingDetailsFragment : Fragment() {

    private var binding: FragmentParkingDetailsBinding? = null
    private var searchResult: SearchResult? = null
    private var entity: Entity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentParkingDetailsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (searchResult?.address.isNullOrEmpty()) {
            binding?.entityAddress?.text = ""
        } else {
            binding?.entityAddress?.text = searchResult?.address
        }

        if (entity?.facets?.openHours?.isOpenNow == true) {
            binding?.entityAlwaysClosed?.text = getString(R.string.open)
            binding?.entityAlwaysClosed?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.green_c1
                )
            )
        } else {
            binding?.entityAlwaysClosed?.text = getString(R.string.perm_closed)
            binding?.entityAlwaysClosed?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.red_c1
                )
            )
        }

        val regularOpenHours = entity?.facets?.openHours?.regularOpenHours
        if (!regularOpenHours.isNullOrEmpty()) {
            val openTime = regularOpenHours[0].openTime
            if (!openTime.isNullOrEmpty()) {
                val startTime = openTime[0].from.split(":")
                val from = startTime[0] + ":" + startTime[1]
                val endTime = openTime[0].to.split(":")
                val to = endTime[0] + ":" + endTime[1]
                val openHours = "$from - $to"

                binding?.entityOpenHours?.text = openHours
                binding?.entityOpenHours?.visibility = View.VISIBLE
                binding?.entityOpenHoursHeader?.visibility = View.VISIBLE
            }
        }


        val prices = searchResult?.parking?.pricing?.prices
        val uniquePrices = ArrayList<ParkingPriceItem>()

        prices?.forEach { price ->
            if (uniquePrices.find { unique -> unique.unitText == price.unitText } == null) {
                uniquePrices.add(price)
            }
        }

        var priceStr = ""
        uniquePrices.forEachIndexed { _, price ->
            if (priceStr.isNotEmpty()) {
                priceStr += ", "
            }
            priceStr += "${price.amount}${String.format("%s", price.symbol)}"
            price.unitText?.let {
                priceStr += " / $it"
            }
        }
        binding?.entityPrice?.text = priceStr
        searchResult?.parking?.spacesAvailable?.let {
            binding?.availableParkingLot?.text = it.toString()
        }

        val imagesId = arrayListOf<Int>()

        entity?.let {
            val amenities = it.facets?.amenities ?: java.util.ArrayList()
            for (value in amenities) {
                when (value.id.toInt()) {
                    1 -> {
                        imagesId.add(R.drawable.ic_handicap_accessible)
                    }
                    2 -> {
                        imagesId.add(R.drawable.ic_electrical_vehicle_charge)
                    }
                    5 -> {
                        imagesId.add(R.drawable.ic_valet)
                    }
                    8 -> {
                        imagesId.add(R.drawable.ic_security_camera)
                    }
                }
            }
        }

        for (id in imagesId) {
            val imageView = ImageView(requireContext())
            imageView.setImageResource(id)
            val params = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            params.setMargins(convertDpToPixel(requireContext(), 10f), 0, 0, 0)
            imageView.layoutParams = params
            binding?.entityAvailableServiceLinerLayout?.addView(imageView)
        }

        if (imagesId.isEmpty()) {
            binding?.entityAvailableServiceHeader?.visibility = View.GONE
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(searchResult: SearchResult?, entity: Entity?) =
            ParkingDetailsFragment().apply {
                this.searchResult = searchResult
                this.entity = entity
            }
    }
}

