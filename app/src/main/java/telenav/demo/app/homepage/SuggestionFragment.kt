package telenav.demo.app.homepage

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.telenav.sdk.entity.android.client.api.AndroidEntityService
import com.telenav.sdk.entity.api.Callback
import com.telenav.sdk.entity.api.EntityClient
import com.telenav.sdk.entity.model.prediction.EntitySuggestionPredictionResponse
import com.telenav.sdk.entity.model.prediction.SuggestionType
import telenav.demo.app.R
import telenav.demo.app.entitydetails.EntityDetailsActivity
import telenav.demo.app.searchlist.SearchListFragment

class SuggestionFragment : Fragment() {
    private val telenavService: EntityClient by lazy { AndroidEntityService.getClient() }
    private lateinit var vSuggestions: View
    private lateinit var vSuggestionsList: RecyclerView
    private lateinit var vSuggestionsLoading: ContentLoadingProgressBar
    private lateinit var vSuggestionsError: TextView
    private lateinit var vSuggestionsEmpty: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_suggestions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vSuggestions = view.findViewById(R.id.suggestions)
        vSuggestionsList = view.findViewById(R.id.suggestionsList)
        vSuggestionsLoading = view.findViewById(R.id.suggestionsLoading)
        vSuggestionsError = view.findViewById(R.id.suggestionsError)
        vSuggestionsEmpty = view.findViewById(R.id.suggestionsEmpty)

        vSuggestionsList.layoutManager = LinearLayoutManager(activity)
        requestSuggestions()
    }

    private fun requestSuggestions() {
        val text = arguments!!.getString("text")
        val location = (activity!! as HomePageActivity).lastKnownLocation ?: Location("")
        Log.w("test", "${location.latitude} ${location.longitude}")

        telenavService.suggestionPredictionRequest()
            .setQuery(text)
            .setLocation(location.latitude, location.longitude)
            .setLimit(10)
            .asyncCall(activity!!.getUIExecutor(),
                object : Callback<EntitySuggestionPredictionResponse> {
                    override fun onSuccess(response: EntitySuggestionPredictionResponse) {
                        if (activity == null)
                            return
                        Log.w("test", Gson().toJson(response.results))
                        vSuggestionsLoading.hide()
                        if (response.results.isEmpty())
                            vSuggestionsEmpty.visibility = View.VISIBLE
                        else {
                            vSuggestions.visibility = View.VISIBLE
                            vSuggestionsList.setAdapter(SuggestionRecyclerAdapter(response.results) { suggestion ->
                                Log.e("test", "click suggestion ${Gson().toJson(suggestion)}")
                                if (suggestion.type == SuggestionType.ENTITY)
                                    activity?.startActivity(
                                        Intent(activity, EntityDetailsActivity::class.java).apply {
                                            putExtra(EntityDetailsActivity.PARAM_ID, suggestion.id)
                                        })
                                else
                                    (activity!! as HomePageActivity).showSearchFragment(
                                        SearchListFragment.newInstance(suggestion)
                                    )
                            })
                        }
                    }

                    override fun onFailure(error: Throwable) {
                        vSuggestionsLoading.hide()
                        vSuggestionsError.visibility = View.VISIBLE
                        Log.e("test", "", error)
                    }
                })
    }

    companion object {
        @JvmStatic
        fun newInstance(text: String) =
            SuggestionFragment().apply {
                arguments = Bundle().apply {
                    putString("text", text)
                }
            }
    }

}