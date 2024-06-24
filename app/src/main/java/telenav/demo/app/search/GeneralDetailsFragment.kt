package telenav.demo.app.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.telenav.sdk.entity.model.base.Entity
import telenav.demo.app.R
import telenav.demo.app.databinding.FragmentGeneralDetailsBinding
import telenav.demo.app.model.SearchResult
import telenav.demo.app.utils.CategoryAndFiltersUtil

class GeneralDetailsFragment : Fragment() {

    private var binding: FragmentGeneralDetailsBinding? = null
    private var searchResult: SearchResult? = null
    private var entity: Entity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGeneralDetailsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (searchResult?.address.isNullOrEmpty()) {
            binding?.entityAddress?.text = ""
        } else {
            binding?.entityAddress?.text = searchResult?.address
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

        if (searchResult?.phoneNo.isNullOrEmpty()) {
            binding?.entityPhoneNumber?.text = ""
            binding?.entityPhoneNumberHeader?.visibility = View.GONE
        } else {
            binding?.entityPhoneNumber?.text = searchResult?.phoneNo
            binding?.entityPhoneNumberHeader?.visibility = View.VISIBLE
        }

        if (entity?.facets?.openHours?.isOpenNow == true) {
            binding?.entityAlwaysClosed?.text = getString(R.string.open)
            binding?.entityAlwaysClosed?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_c1))
        } else {
            binding?.entityAlwaysClosed?.text = getString(R.string.perm_closed)
            binding?.entityAlwaysClosed?.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_c1))
        }

        val rating = entity?.facets?.rating
        if (!rating.isNullOrEmpty()) {
            binding?.entityRoot?.visibility = View.VISIBLE
            binding?.entityReviewCount?.visibility = View.VISIBLE
            binding?.entityReviewCount?.text = getString(R.string.reviews, rating[0].totalCount)
            CategoryAndFiltersUtil.setYelpStarsViewBasedOnRating( binding?.entityRoot!!, rating[0].averageRating, requireContext())
        } else {
            binding?.entityRoot?.visibility = View.GONE
            binding?.entityReviewCount?.visibility = View.GONE
        }

    }

    fun updateStartsVisibility(slideOffset: Float) {
        if (slideOffset == 0f) {
            binding?.entityStars?.visibility = View.GONE
            binding?.entityReviewCount?.visibility = View.GONE
        } else {
            binding?.entityStars?.visibility = View.VISIBLE
            binding?.entityReviewCount?.visibility = View.VISIBLE
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(searchResult: SearchResult?, entity: Entity?) =
            GeneralDetailsFragment().apply {
                this.searchResult = searchResult
                this.entity = entity
            }
    }
}

