package telenav.demo.app.personalinfo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ContentLoadingProgressBar
import telenav.demo.app.R

class PersonalInfoActivity : AppCompatActivity() {

    private lateinit var vLoading: ContentLoadingProgressBar
    private lateinit var vHomeAddress: TextView
    private lateinit var vWorkAddress: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info)

        vLoading = findViewById(R.id.personal_info_loading)
        vHomeAddress = findViewById(R.id.personal_home_address)
        vWorkAddress = findViewById(R.id.personal_work_address)

//        vLoading.show()
        findViewById<View>(R.id.personal_info_ota).setOnClickListener { showHomeAreaActivity() }
        findViewById<View>(R.id.personal_info_back).setOnClickListener { finish() }
    }

    private fun showHomeAreaActivity() {
        startActivity(Intent(this, HomeAreaActivity::class.java))
    }
}