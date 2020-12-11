package telenav.demo.app.utils

import android.content.Context
import com.google.gson.Gson
import com.telenav.sdk.datacollector.api.DataCollectorClient
import com.telenav.sdk.datacollector.model.event.*
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.GeoPoint
import telenav.demo.app.R

fun Entity.getCoord(): GeoPoint? {
    return address?.geoCoordinates ?: place?.address?.geoCoordinates
}

fun DataCollectorClient.startEngine() {
    sendEventRequest()
        .setEvent(StartEngineEvent.builder().build()).build().execute()
}

fun DataCollectorClient.stopEngine() {
    sendEventRequest()
        .setEvent(StopEngineEvent.builder().build()).build().execute()
}

fun DataCollectorClient.removeAllFavorites(context: Context) {
    val prefs =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )
    with(prefs.edit()) {
        remove(context.getString(telenav.demo.app.R.string.saved_favorite_list_key))
        apply()
    }

    sendEventRequest()
        .setEvent(RemoveAllFavoritesEvent.builder().build()).build().execute()
}

fun DataCollectorClient.setHome(context: Context, entity: Entity) {
    val coords = entity.getCoord();
    val eventBuilder =
        SetHomeEvent.builder()
            .setLabel(entity.label).setEntityId(entity.id)
            .setActionType(SetHomeEvent.ActionType.SET)
    if (coords != null) {
        eventBuilder.setLat(coords.latitude).setLon(coords.longitude)
    }

    val setEvent = eventBuilder.build()
    val removeEvent = SetHomeEvent.builder().setActionType(SetHomeEvent.ActionType.REMOVE).build()

    setAddress(
        context,
        entity,
        context.getString(R.string.saved_home_address_key),
        setEvent,
        removeEvent
    )
}

fun DataCollectorClient.removeHome(context: Context) {
    val prefs =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )
    with(prefs.edit()) {
        remove(context.getString(R.string.saved_home_address_key))
        apply()
    }

    sendEventRequest()
        .setEvent(SetHomeEvent.builder().setActionType(SetHomeEvent.ActionType.REMOVE).build()).build().execute()
}

fun DataCollectorClient.setWork(context: Context, entity: Entity) {
    val coords = entity.getCoord();
    val eventBuilder =
        SetWorkEvent.builder()
            .setLabel(entity.label).setEntityId(entity.id)
            .setActionType(SetWorkEvent.ActionType.SET)
    if (coords != null) {
        eventBuilder.setLat(coords.latitude).setLon(coords.longitude)
    }

    val setEvent = eventBuilder.build()
    val removeEvent = SetWorkEvent.builder().setActionType(SetWorkEvent.ActionType.REMOVE).build()

    setAddress(
        context,
        entity,
        context.getString(R.string.saved_work_address_key),
        setEvent,
        removeEvent
    )
}

fun DataCollectorClient.removeWork(context: Context) {
    val prefs =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )
    with(prefs.edit()) {
        remove(context.getString(R.string.saved_work_address_key))
        apply()
    }

    sendEventRequest()
        .setEvent(SetWorkEvent.builder().setActionType(SetWorkEvent.ActionType.REMOVE).build()).build().execute()
}

private fun DataCollectorClient.setAddress(
    context: Context,
    entity: Entity,
    prefsKey: String,
    setEvent: Event,
    removeEvent: Event
) {
    val prefs =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )

    val modify = prefs.contains(prefsKey)

    with(prefs.edit()) {
        putString(prefsKey, Gson().toJson(entity))
        apply()
    }

    if (modify) {
        sendEventRequest()
            .setEvent(removeEvent)
            .build().execute()
    }

    sendEventRequest().setEvent(setEvent).build().execute()
}