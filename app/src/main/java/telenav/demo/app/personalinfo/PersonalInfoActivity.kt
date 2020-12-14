package telenav.demo.app.personalinfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ContentLoadingProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.telenav.sdk.datacollector.api.DataCollectorService
import com.telenav.sdk.datacollector.model.event.EntityCacheActionEvent
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.EntityType
import telenav.demo.app.R
import telenav.demo.app.entitydetails.EntityDetailsActivity
import telenav.demo.app.utils.*
import java.lang.reflect.Type


class PersonalInfoActivity : AppCompatActivity() {
    private val dataCollectorClient by lazy { DataCollectorService.getClient() }

    private lateinit var vLoading: ContentLoadingProgressBar

    private lateinit var vHomeEmpty: TextView
    private lateinit var vHomeEntity: View
    private lateinit var vHomeName: TextView
    private lateinit var vHomeAddress: TextView
    private lateinit var vHomeEntityStars: View
    private lateinit var vHomeEntityRating: TextView
    private lateinit var vHomeEntityYelpSign: View
    private lateinit var vHomeEntityStar: ArrayList<ImageView>

    private lateinit var vWorkEmpty: TextView
    private lateinit var vWorkEntity: View
    private lateinit var vWorkName: TextView
    private lateinit var vWorkAddress: TextView
    private lateinit var vWorkEntityStars: View
    private lateinit var vWorkEntityRating: TextView
    private lateinit var vWorkEntityYelpSign: View
    private lateinit var vWorkEntityStar: ArrayList<ImageView>

    private lateinit var vFavoriteEmpty: TextView
    private lateinit var vFavoriteList: RecyclerView
    private lateinit var vFavoriteListContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info)

        vLoading = findViewById(R.id.personal_info_loading)

        vHomeEmpty = findViewById(R.id.personal_home_empty)
        vHomeEntity = findViewById(R.id.personal_home_entity)
        vHomeName = findViewById(R.id.personal_home_entity_name)
        vHomeAddress = findViewById(R.id.personal_home_entity_address)
        vHomeEntityStars = findViewById(R.id.personal_home_entity_stars)
        vHomeEntityRating = findViewById(R.id.personal_home_entity_rating)
        vHomeEntityYelpSign = findViewById(R.id.personal_home_entity_yelp_sign)
        vHomeEntityStar = ArrayList<ImageView>().apply {
            add(findViewById(R.id.personal_home_entity_star1))
            add(findViewById(R.id.personal_home_entity_star2))
            add(findViewById(R.id.personal_home_entity_star3))
            add(findViewById(R.id.personal_home_entity_star4))
            add(findViewById(R.id.personal_home_entity_star5))
        }

        vWorkEmpty = findViewById(R.id.personal_work_empty)
        vWorkEntity = findViewById(R.id.personal_work_entity)
        vWorkName = findViewById(R.id.personal_work_entity_name)
        vWorkAddress = findViewById(R.id.personal_work_entity_address)
        vWorkEntityStars = findViewById(R.id.personal_work_entity_stars)
        vWorkEntityRating = findViewById(R.id.personal_work_entity_rating)
        vWorkEntityYelpSign = findViewById(R.id.personal_work_entity_yelp_sign)
        vWorkEntityStar = ArrayList<ImageView>().apply {
            add(findViewById(R.id.personal_work_entity_star1))
            add(findViewById(R.id.personal_work_entity_star2))
            add(findViewById(R.id.personal_work_entity_star3))
            add(findViewById(R.id.personal_work_entity_star4))
            add(findViewById(R.id.personal_work_entity_star5))
        }

        vFavoriteEmpty = findViewById(R.id.personal_favorite_empty)
        vFavoriteList = findViewById(R.id.personal_favorite_list)
        vFavoriteListContainer = findViewById(R.id.personal_favorite_list_container)

        vFavoriteList.layoutManager = LinearLayoutManager(this)

        vLoading.show()
        findViewById<View>(R.id.personal_info_ota).setOnClickListener { showHomeAreaActivity() }
        findViewById<View>(R.id.personal_info_back).setOnClickListener { finish() }

        findViewById<View>(R.id.personal_home_delete).setOnClickListener { deleteHomeData() }
        findViewById<View>(R.id.personal_work_delete).setOnClickListener { deleteWorkData() }
        findViewById<View>(R.id.personal_favorite_delete_all).setOnClickListener { deleteAllFavoriteEntities() }
    }

    override fun onResume() {
        super.onResume()
        getPersonalData()
    }

    private fun showHomeAreaActivity() {
        startActivity(Intent(this, HomeAreaActivity::class.java))
    }

    private fun deleteHomeData() {
        dataCollectorClient.removeHome(this)
        getPersonalData()
    }

    private fun deleteWorkData() {
        dataCollectorClient.removeWork(this)
        getPersonalData()
    }

    private fun deleteAllFavoriteEntities() {
        dataCollectorClient.removeAllFavorites(this)
        getPersonalData()
    }

    private fun deleteFavoriteEntity(entity: Entity) {
        dataCollectorClient.deleteFavorite(this, entity)
        getPersonalData()
    }

    private fun getPersonalData() {
        val prefs =
            getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val storedHome = Gson().fromJson(
            prefs.getString(getString(R.string.saved_home_address_key), ""),
            Entity::class.java
        )
        fillHomeInfo(storedHome)

        val storedWork = Gson().fromJson(
            prefs.getString(getString(R.string.saved_work_address_key), ""),
            Entity::class.java
        )
        fillWorkInfo(storedWork)

        val listType: Type = object : TypeToken<List<Entity>>() {}.type
        val favoriteEntities = Gson().fromJson<List<Entity>>(
            prefs.getString(
                getString(R.string.saved_favorite_list_key),
                ""
            ), listType
        )
        fillFavoriteList(favoriteEntities)
    }

    private fun fillHomeInfo(entity: Entity?) {
        if (entity == null) {
            vHomeEmpty.visibility = View.VISIBLE
            vHomeEntity.visibility = View.GONE
            return
        }
        vHomeEmpty.visibility = View.GONE
        vHomeEntity.visibility = View.VISIBLE

        val name =
            if (entity.type == EntityType.ADDRESS) entity.address.formattedAddress else entity.place.name

        vHomeName.text = name
        vHomeEntity.setOnClickListener {
            dataCollectorClient.entityCachedClick(entity.id, EntityCacheActionEvent.SourceType.HOME)
            startActivity(
                Intent(
                    this,
                    EntityDetailsActivity::class.java
                ).apply {
                    putExtra(EntityDetailsActivity.PARAM_ID, entity.id)
                    putExtra(EntityDetailsActivity.PARAM_SOURCE, EntityCacheActionEvent.SourceType.HOME.name)
                })
        }
        if (entity.type == EntityType.ADDRESS) {
            vHomeAddress.visibility = View.GONE
        } else {
            vHomeAddress.text = entity.place.address.formattedAddress
            vHomeAddress.visibility = View.VISIBLE
        }

        if (entity.facets?.rating != null && entity.facets?.rating!!.size > 0) {
            val rating = entity.facets?.rating!![0]
            vHomeEntityStars.visibility = View.VISIBLE
            vHomeEntityYelpSign.visibility =
                if (rating.source == "YELP") View.VISIBLE else View.GONE
            for (i in 0..5) {
                if (rating.averageRating >= i + 1) {
                    vHomeEntityStar[i].setImageResource(R.drawable.ic_star_full)
                } else if (rating.averageRating > i) {
                    vHomeEntityStar[i].setImageResource(R.drawable.ic_start_half)
                }
            }

            vHomeEntityRating.text =
                if (rating.source == "YELP")
                    "${rating.totalCount} Yelp reviews"
                else
                    "${rating.totalCount} reviews"

        }
    }

    private fun fillWorkInfo(entity: Entity?) {
        if (entity == null) {
            vWorkEmpty.visibility = View.VISIBLE
            vWorkEntity.visibility = View.GONE
            return
        }
        vWorkEmpty.visibility = View.GONE
        vWorkEntity.visibility = View.VISIBLE

        val name =
            if (entity.type == EntityType.ADDRESS) entity.address.formattedAddress else entity.place.name

        vWorkName.text = name
        vWorkEntity.setOnClickListener {
            dataCollectorClient.entityCachedClick(entity.id, EntityCacheActionEvent.SourceType.WORK)
            startActivity(
                Intent(
                    this,
                    EntityDetailsActivity::class.java
                ).apply {
                    putExtra(EntityDetailsActivity.PARAM_ID, entity.id)
                    putExtra(EntityDetailsActivity.PARAM_SOURCE, EntityCacheActionEvent.SourceType.WORK.name)
                })
        }
        if (entity.type == EntityType.ADDRESS) {
            vWorkAddress.visibility = View.GONE
        } else {
            vWorkAddress.text = entity.place.address.formattedAddress
            vWorkAddress.visibility = View.VISIBLE
        }

        if (entity.facets?.rating != null && entity.facets?.rating!!.size > 0) {
            val rating = entity.facets?.rating!![0]
            vWorkEntityStars.visibility = View.VISIBLE
            vWorkEntityYelpSign.visibility =
                if (rating.source == "YELP") View.VISIBLE else View.GONE
            for (i in 0..5) {
                if (rating.averageRating >= i + 1) {
                    vWorkEntityStar[i].setImageResource(R.drawable.ic_star_full)
                } else if (rating.averageRating > i) {
                    vWorkEntityStar[i].setImageResource(R.drawable.ic_start_half)
                }
            }

            vWorkEntityRating.text =
                if (rating.source == "YELP")
                    "${rating.totalCount} Yelp reviews"
                else
                    "${rating.totalCount} reviews"

        }
    }

    private fun fillFavoriteList(favoriteEntities: List<Entity>?) {
        if (favoriteEntities == null || favoriteEntities.isEmpty()) {
            vFavoriteListContainer.visibility = View.GONE
            vFavoriteEmpty.visibility = View.VISIBLE
            return
        }
        vFavoriteList.adapter = FavoriteResultsListRecyclerAdapter(
            favoriteEntities,
            object : OnDeleteFavoriteResultListener {
                override fun onDelete(entity: Entity) {
                    deleteFavoriteEntity(entity)
                }
            }
        )
        vFavoriteEmpty.visibility = View.GONE
        vFavoriteListContainer.visibility = View.VISIBLE
    }
}