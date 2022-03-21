package telenav.demo.app.search

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import telenav.demo.app.R
import telenav.demo.app.databinding.FragmentMoreCategoriesBinding
import telenav.demo.app.utils.Converter
import telenav.demo.app.widgets.RoundedBottomSheetLayout
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.telenav.sdk.entity.model.base.Category
import telenav.demo.app.homepage.getUIExecutor
import telenav.demo.app.map.MapActivity

class MoreCategoriesFragment : RoundedBottomSheetLayout() {

    private var binding: FragmentMoreCategoriesBinding? = null
    private val viewModel: SearchInfoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMoreCategoriesBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.getUIExecutor()?.let { executor ->
            viewModel.requestCategories(executor)
        }

        viewModel.categories.observe(viewLifecycleOwner, {
            for (category in it) {
                setupChipGroupDynamically(category.name, category.childNodes)
            }
        })

        viewModel.loading.observe(viewLifecycleOwner, {
            if (it) {
                binding?.loadingView?.visibility = View.VISIBLE
            } else {
                binding?.loadingView?.visibility = View.GONE
            }
        })

        viewModel.searchError.observe(viewLifecycleOwner, {
            if (!it.isNullOrBlank()) {
                binding?.searchErrorView?.visibility = View.VISIBLE
                binding?.searchError?.text = it
            } else {
                binding?.searchErrorView?.visibility = View.GONE
            }
        })

        binding?.moreCategoriesAreaBack?.setOnClickListener {
            (activity!! as MapActivity).expandBottomSheet()
            dismiss()
        }
    }

    private fun setupChipGroupDynamically(header: String, list: List<Category>) {
        val chipGroup = ChipGroup(requireContext())
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        val margin = Converter.convertDpToPixel(requireContext(), 20f)

        layoutParams.setMargins(margin , 0, 0 , margin)
        chipGroup.layoutParams = layoutParams

        chipGroup.isSingleSelection = true
        chipGroup.isSingleLine = false
        chipGroup.layoutParams

        for (category in list) {
            val chip = getChip(category.name)
            chipGroup.addView(chip as View)
        }
        setHeader(header)
        binding?.moreCategoryRoot?.addView(chipGroup)
    }

    private fun getChip(name: String): Chip {
        val chip = Chip(requireContext())
        val margin = Converter.convertDpToPixel(requireContext(), 20f)

        chip.text = name
        chip.textAlignment = View.TEXT_ALIGNMENT_CENTER
        chip.chipBackgroundColor =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                R.color.grey_c2))
        chip.isClickable = true
        chip.isCheckable = false
        chip.isCloseIconVisible = false
        chip.textStartPadding = margin.toFloat()
        chip.textEndPadding = margin.toFloat()
        chip.setOnClickListener {
            (activity!! as MapActivity).showSearchInfoBottomFragment(chip.text.toString(), "")
            dismiss()
        }
        return chip
    }

    private fun setHeader(header: String) {
        val headerTextView = TextView(requireContext())
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        val margin = Converter.convertDpToPixel(requireContext(), 20f)

        layoutParams.setMargins(margin , 0, 0 , 0)
        headerTextView.layoutParams = layoutParams

        headerTextView.textSize = 14f
        headerTextView.text = header
        headerTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_c1))
        binding?.moreCategoryRoot?.addView(headerTextView)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MoreCategoriesFragment()
    }
}

