package telenav.demo.app.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

        if (!searchResult?.hours.isNullOrEmpty()) {
            binding?.entityOpenHours?.text = searchResult?.hours
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

