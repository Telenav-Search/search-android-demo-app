package telenav.demo.app.personalinfo

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.datacollector.model.event.EntityCacheActionEvent
import com.telenav.sdk.entity.model.base.EntityType
import telenav.demo.app.R
import telenav.demo.app.utils.entityCachedClick
import telenav.demo.app.databinding.FragmentUserAddressBinding
import telenav.demo.app.map.MapActivity
import telenav.demo.app.utils.*

private const val TAG = "UserAddressFragment"

class UserAddressFragment : Fragment() {

    private var binding : FragmentUserAddressBinding? = null
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserAddressBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fillHomeInfo()
        fillWorkInfo()

        binding?.homeAddressConfigure?.setOnClickListener {
            (activity!! as MapActivity).showSearchListBottomFragmentFromUserAddress(
                shouldUpdateWorkAddress = false,
                shouldUpdateHomeAddress = true
            )
        }
        binding?.workAddressConfigure?.setOnClickListener {
            (activity!! as MapActivity).showSearchListBottomFragmentFromUserAddress(
                shouldUpdateWorkAddress = true,
                shouldUpdateHomeAddress = false
            )
        }
    }

    private fun fillHomeInfo() {
        val storedHome = dataCollectorClient.getHome(requireContext())

        if (storedHome == null) {
            binding?.homeAddressConfigure?.text = requireContext().getString(R.string.setup)
            return
        } else {
            binding?.homeAddressConfigure?.text = requireContext().getString(R.string.edit)
        }

        val name = if (storedHome.place != null) {
            storedHome.place?.address?.formattedAddress
        } else {
            storedHome.address?.formattedAddress
        }

        binding?.homeAddress?.text = name
        binding?.homeAddress?.setOnClickListener {
            dataCollectorClient.entityCachedClick(storedHome.id, EntityCacheActionEvent.SourceType.HOME)
        }
    }

    private fun fillWorkInfo() {
        val storedWork = dataCollectorClient.getWork(requireContext())

        if (storedWork == null) {
            binding?.workAddressConfigure?.text = requireContext().getString(R.string.setup)
            return
        } else {
            binding?.workAddressConfigure?.text = requireContext().getString(R.string.edit)
        }

        val name = if (storedWork.place != null) {
            storedWork.place?.address?.formattedAddress
        } else {
            storedWork.address?.formattedAddress
        }

        binding?.workAddress?.text = name
        binding?.workAddress?.setOnClickListener {
            dataCollectorClient.entityCachedClick(storedWork.id, EntityCacheActionEvent.SourceType.WORK)
        }
    }

    companion object {
        fun newInstance() = UserAddressFragment()
    }

}