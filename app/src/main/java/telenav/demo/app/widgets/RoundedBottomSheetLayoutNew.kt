package telenav.demo.app.widgets

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import telenav.demo.app.R

open class RoundedBottomSheetLayoutNew : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.BottomSheetDialogThemeNew

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        BottomSheetDialog(requireContext(), theme).apply {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
        }
}
