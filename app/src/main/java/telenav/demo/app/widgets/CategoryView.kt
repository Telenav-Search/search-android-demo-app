package telenav.demo.app.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import android.view.LayoutInflater
import com.telenav.sdk.entity.model.base.Category
import telenav.demo.app.databinding.CategoryViewBinding
import telenav.demo.app.homepage.HotCategory

class CategoryView : ConstraintLayout {

    private var binding: CategoryViewBinding? = null

    constructor(context: Context) : super(context) {
        inflateView(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflateView(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflateView(context)
    }

    private fun inflateView(context: Context) {
        binding = CategoryViewBinding.inflate(LayoutInflater.from(context))
        addView(binding?.root)
    }

    fun init(category: HotCategory) {
        binding?.categoryName?.text = category.name
        binding?.categoryIcon?.setImageResource(category.iconPurple)
    }

    fun init(category: Category) {
        binding?.categoryName?.text = category.name
    }

}