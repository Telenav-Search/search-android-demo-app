package telenav.demo.app.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.FacetEvConnectors
import telenav.demo.app.R
import telenav.demo.app.databinding.FragmentEvChargerDetailsBinding
import telenav.demo.app.model.SearchResult

class EvChargerDetailsFragment : Fragment() {

    private var binding: FragmentEvChargerDetailsBinding? = null
    private var searchResult: SearchResult? = null
    private var entity: Entity? = null

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

        if (entity?.facets?.openHours?.isOpenNow == true) {
            binding?.entityAlwaysClosed?.text = getString(R.string.open)
            binding?.entityAlwaysClosed?.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_c1))
        } else {
            binding?.entityAlwaysClosed?.text = getString(R.string.perm_closed)
            binding?.entityAlwaysClosed?.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_c1))
        }

        val facetEvConnectors: FacetEvConnectors? = entity?.facets?.evConnectors
        var entityConnectorsType = ""
        var entityPowerFeed = ""
        var entityChargerBrand = ""
        val maxCount = 4
        var connectorsCount = 0
        var powerFeedCount = 0
        var chargerBrandCount = 0

        facetEvConnectors?.let {
            it.connectors.forEachIndexed { index, entity ->
                val powerFeed = entity.powerFeedLevel.name
                val chargerBrand = entity.chargerBrand.brandName
                val connectorType = entity.connectorType.name

                connectorType?.let {
                    if (!entityConnectorsType.contains(it) && connectorsCount < maxCount) {
                        if (index != 0) {
                            entityConnectorsType += ", "
                        }
                        entityConnectorsType += it
                        connectorsCount++
                    }
                }
                chargerBrand?.let {
                    if (!entityChargerBrand.contains(it) && powerFeedCount < maxCount) {
                        if (index != 0) {
                            entityChargerBrand += ", "
                        }
                        entityChargerBrand += it
                        powerFeedCount++
                    }
                }
                powerFeed?.let {
                    if (!entityPowerFeed.contains(it) && chargerBrandCount < maxCount) {
                        if (index != 0) {
                            entityPowerFeed += ", "
                        }
                        entityPowerFeed += it
                        chargerBrandCount++
                    }
                }
            }
        }

        binding?.entityConnectorsNumber?.text = facetEvConnectors?.totalNumber.toString()
        binding?.entityConnectorsType?.text = entityConnectorsType
        binding?.entityPowerFeed?.text = entityPowerFeed
        binding?.entityChargerBrand?.text = entityChargerBrand

    }

    companion object {
        @JvmStatic
        fun newInstance(searchResult: SearchResult?, entity: Entity?) =
            EvChargerDetailsFragment().apply {
                this.searchResult = searchResult
                this.entity = entity
            }
    }
}

