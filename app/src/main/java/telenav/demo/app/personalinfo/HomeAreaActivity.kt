package telenav.demo.app.personalinfo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ContentLoadingProgressBar
import telenav.demo.app.R

class HomeAreaActivity : AppCompatActivity() {

    private lateinit var vLoading: ContentLoadingProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_area)

        vLoading = findViewById(R.id.home_area_loading)

//        vLoading.show()
        findViewById<View>(R.id.home_area_back).setOnClickListener { finish() }
    }
}