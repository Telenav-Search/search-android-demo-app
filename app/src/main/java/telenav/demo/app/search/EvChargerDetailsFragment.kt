package telenav.demo.app.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import telenav.demo.app.R
import telenav.demo.app.databinding.FragmentEvChargerDetailsBinding
import telenav.demo.app.model.SearchResult

class EvChargerDetailsFragment : Fragment() {

    private var binding: FragmentEvChargerDetailsBinding? = null
    private var searchResult: SearchResult? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEvChargerDetailsBinding.inflate(inflater, container, false)
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
    }

    companion object {
        @JvmStatic
        fun newInstance(searchResult: SearchResult?) =
            EvChargerDetailsFragment().apply {
                this.searchResult = searchResult
            }
    }
}

