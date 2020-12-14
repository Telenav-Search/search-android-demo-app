package telenav.demo.app.utils

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.telenav.sdk.datacollector.api.DataCollectorClient
import com.telenav.sdk.datacollector.model.event.*
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.GeoPoint
import telenav.demo.app.R
import java.lang.reflect.Type

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

fun DataCollectorClient.addFavorite(context: Context, entity: Entity) {
    val prefs =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )

    val listType: Type = object : TypeToken<ArrayList<Entity>>() {}.type
    var favoriteEntities = Gson().fromJson<ArrayList<Entity>>(
        prefs.getString(
            context.getString(R.string.saved_favorite_list_key),
            ""
        ), listType
    )

    when {
        favoriteEntities == null -> {
            favoriteEntities = arrayListOf(entity)
        }
        favoriteEntities.none { e -> e.id == entity.id } -> {
            favoriteEntities.add(entity)
        }
        else -> {
            return
        }
    }

    with(prefs.edit()) {
        putString(
            context.getString(R.string.saved_favorite_list_key),
            Gson().toJson(favoriteEntities)
        )
        apply()
    }

    sendEventRequest().setEvent(
        FavoriteEvent.builder().setEntityId(entity.id)
            .setActionType(FavoriteEvent.ActionType.ADD).build()
    ).build().execute()
}

fun DataCollectorClient.deleteFavorite(context: Context, entity: Entity) {
    val prefs =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )

    val listType: Type = object : TypeToken<ArrayList<Entity>>() {}.type
    val favoriteEntities = Gson().fromJson<ArrayList<Entity>>(
        prefs.getString(
            context.getString(R.string.saved_favorite_list_key),
            ""
        ), listType
    )

    if (favoriteEntities == null || favoriteEntities.none { e -> e.id == entity.id }) {
        return
    } else {
        with(prefs.edit()) {
            putString(
                context.getString(R.string.saved_favorite_list_key),
                Gson().toJson(favoriteEntities.filter { e -> e.id != entity.id })
            )
            apply()
        }
    }

    sendEventRequest().setEvent(
        FavoriteEvent.builder().setEntityId(entity.id)
            .setActionType(FavoriteEvent.ActionType.DELETE).build()
    ).build().execute()
}

fun DataCollectorClient.removeAllFavorites(context: Context) {
    val prefs =
        context.getSharedPreferences(
            context.getString(R.string.preference_file_key),
            Context.MODE_PRIVATE
        )


    val listType: Type = object : TypeToken<ArrayList<Entity>>() {}.type
    val favoriteEntities = Gson().fromJson<ArrayList<Entity>>(
        prefs.getString(
            context.getString(R.string.saved_favorite_list_key),
            ""
        ), listType
    )
    if (favoriteEntities == null || favoriteEntities.isEmpty()) {
        return
    }

    with(prefs.edit()) {
        remove(context.getString(R.string.saved_favorite_list_key))
        apply()
    }

    sendEventRequest()
        .setEvent(RemoveAllFavoritesEvent.builder().build()).build().execute()
}

fun DataCollectorClient.setHome(context: Context, entity: Entity) {
    val eventBuilder = getHomeEventBuilder(entity)

    val setEvent = eventBuilder.setActionType(SetHomeEvent.ActionType.SET).build()
    val removeEvent = eventBuilder.setActionType(SetHomeEvent.ActionType.REMOVE).build()

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
    val entity = Gson().fromJson(
        prefs.getString(context.getString(R.string.saved_home_address_key), ""),
        Entity::class.java
    )

    entity ?: return

    with(prefs.edit()) {
        remove(context.getString(R.string.saved_home_address_key))
        apply()
    }

    sendEventRequest()
        .setEvent(getHomeEventBuilder(entity).setActionType(SetHomeEvent.ActionType.REMOVE).build())
        .build().execute()
}

fun DataCollectorClient.setWork(context: Context, entity: Entity) {
    val eventBuilder = getWorkEventBuilder(entity)

    val setEvent = eventBuilder.setActionType(SetWorkEvent.ActionType.SET).build()
    val removeEvent = eventBuilder.setActionType(SetWorkEvent.ActionType.REMOVE).build()

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
    val entity = Gson().fromJson(
        prefs.getString(context.getString(R.string.saved_work_address_key), ""),
        Entity::class.java
    )

    entity ?: return

    with(prefs.edit()) {
        remove(context.getString(R.string.saved_work_address_key))
        apply()
    }

    sendEventRequest()
        .setEvent(getWorkEventBuilder(entity).setActionType(SetWorkEvent.ActionType.REMOVE).build())
        .build().execute()
}

private fun getHomeEventBuilder(entity: Entity): SetHomeEvent.Builder {
    val coords = entity.getCoord();
    val eventBuilder =
        SetHomeEvent.builder()
            .setLabel(entity.label ?: "").setEntityId(entity.id)
    if (coords != null) {
        eventBuilder.setLat(coords.latitude).setLon(coords.longitude)
    }

    return eventBuilder
}

private fun getWorkEventBuilder(entity: Entity): SetWorkEvent.Builder {
    val coords = entity.getCoord();
    val eventBuilder =
        SetWorkEvent.builder()
            .setLabel(entity.label ?: "").setEntityId(entity.id)
    if (coords != null) {
        eventBuilder.setLat(coords.latitude).setLon(coords.longitude)
    }

    return eventBuilder
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

fun DataCollectorClient.entityClick(entityId: String, displayMode: EntityActionEvent.DisplayMode) {
    sendEventRequest().setEvent(
        EntityActionEvent.builder().setActionType(EntityActionEvent.ActionType.CLICK)
            .setEntityId(entityId).setDisplayMode(displayMode).build()
    ).build().execute()
}

fun DataCollectorClient.entityCall(entityId: String, displayMode: EntityActionEvent.DisplayMode) {
    sendEventRequest().setEvent(
        EntityActionEvent.builder().setActionType(EntityActionEvent.ActionType.CALL)
            .setEntityId(entityId).setDisplayMode(displayMode).build()
    ).build().execute()
}

fun DataCollectorClient.entityCachedClick(
    entityId: String,
    source: EntityCacheActionEvent.SourceType
) {
    Log.d("entityCachedClick", entityId)
    Log.d("entityCachedClick", source.name)
//    sendEventRequest().setEvent(
//        EntityCacheActionEvent.builder().setActionType(EntityCacheActionEvent.ActionType.CLICK)
//            .setEntityId(entityId).setSourceType(source).build()
//    ).build().execute()
}

fun DataCollectorClient.entityCachedCall(
    entityId: String,
    source: EntityCacheActionEvent.SourceType
) {
    Log.d("entityCachedCall", entityId)
    Log.d("entityCachedCall", source.name)
//    sendEventRequest().setEvent(
//        EntityCacheActionEvent.builder().setActionType(EntityCacheActionEvent.ActionType.CALL)
//            .setEntityId(entityId).setSourceType(source).build()
//    ).build().execute()
}

fun DataCollectorClient.gpsProbe(location: Location?) {
    location ?: return
    sendEventRequest().setEvent(
        GpsProbeEvent.builder()
            .setLat(location.latitude)
            .setLon(location.longitude)
            .setAltitude(location.altitude)
            .setSpeed(location.speed.toDouble())
            .setHorizontalAccuracy(location.accuracy.toDouble())
            .setTimestamp(location.time)
            .setHeadingAngle(location.bearing.toDouble())
            .build()
    ).build().execute()
}