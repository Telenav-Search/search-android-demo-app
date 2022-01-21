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

class ParkingDetailsFragment : Fragment() {

    private var binding: FragmentParkingDetailsBinding? = null
    private var searchResult: SearchResult? = null

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

        if (searchResult?.permanentlyClosed != null) {
            binding?.entityAlwaysClosed?.text = getString(R.string.perm_closed)
            binding?.entityAlwaysClosed?.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_c1))
        } else {
            binding?.entityAlwaysClosed?.text = getString(R.string.open)
            binding?.entityAlwaysClosed?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_c1))
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
    }

    companion object {
        @JvmStatic
        fun newInstance(searchResult: SearchResult?) =
            ParkingDetailsFragment().apply {
                this.searchResult = searchResult
            }
    }
}

