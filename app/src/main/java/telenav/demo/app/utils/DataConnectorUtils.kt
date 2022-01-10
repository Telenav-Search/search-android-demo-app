package telenav.demo.app.utils

import android.location.Location
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.telenav.sdk.core.Callback
import com.telenav.sdk.dataconnector.api.DataConnectorClient
import com.telenav.sdk.dataconnector.model.SendEventResponse
import com.telenav.sdk.dataconnector.model.event.*
import com.telenav.sdk.entity.model.base.Entity
import com.telenav.sdk.entity.model.base.GeoPoint
import java.lang.reflect.Type

fun Entity.getCoord(): GeoPoint? {
    return address?.geoCoordinates ?: place?.address?.geoCoordinates
}

fun DataConnectorClient.startEngine() {
    sendEventRequest().setEvent(StartEngineEvent.builder().build())
        .asyncCall(object : Callback<SendEventResponse> {
            override fun onSuccess(response: SendEventResponse) {
                Log.d("StartEngineEvent", Gson().toJson(response))
            }

            override fun onFailure(e: Throwable) {
                e.printStackTrace()
            }
        })
}

fun DataConnectorClient.stopEngine() {
    sendEventRequest().setEvent(StopEngineEvent.builder().build())
        .asyncCall(object : Callback<SendEventResponse> {
            override fun onSuccess(response: SendEventResponse) {
                Log.d("StopEngineEvent", Gson().toJson(response))
            }

            override fun onFailure(e: Throwable) {
                e.printStackTrace()
            }
        })
}

fun DataConnectorClient.addFavorite(entity: Entity) {
    val sharedPreferencesRepository = SharedPreferencesRepository.getInstance()
    val listType: Type = object : TypeToken<ArrayList<Entity>>() {}.type
    var favoriteEntities = Gson().fromJson<ArrayList<Entity>>(
        sharedPreferencesRepository.favoriteList.value, listType
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

    sharedPreferencesRepository.favoriteList.value = Gson().toJson(favoriteEntities)

    sendEventRequest().setEvent(
        FavoriteEvent.builder().setEntityId(entity.id)
            .setActionType(FavoriteEvent.ActionType.ADD).build()
    ).asyncCall(object : Callback<SendEventResponse> {
        override fun onSuccess(response: SendEventResponse) {
            Log.d("FavoriteEvent.ADD", Gson().toJson(response))
        }

        override fun onFailure(e: Throwable) {
            e.printStackTrace()
        }
    })
}

fun DataConnectorClient.deleteFavorite(entity: Entity) {
    val sharedPreferencesRepository = SharedPreferencesRepository.getInstance()
    val listType: Type = object : TypeToken<ArrayList<Entity>>() {}.type
    val favoriteEntities = Gson().fromJson<ArrayList<Entity>>(
        sharedPreferencesRepository.favoriteList.value,
        listType
    )

    if (favoriteEntities == null || favoriteEntities.none { e -> e.id == entity.id }) {
        return
    } else {
        sharedPreferencesRepository.favoriteList.value =
            Gson().toJson(favoriteEntities.filter { e -> e.id != entity.id })
    }

    sendEventRequest().setEvent(
        FavoriteEvent.builder().setEntityId(entity.id)
            .setActionType(FavoriteEvent.ActionType.DELETE).build()
    ).asyncCall(object : Callback<SendEventResponse> {
        override fun onSuccess(response: SendEventResponse) {
            Log.d("FavoriteEvent.DELETE", Gson().toJson(response))
        }

        override fun onFailure(e: Throwable) {
            e.printStackTrace()
        }
    })
}

fun DataConnectorClient.removeAllFavorites() {
    val sharedPreferencesRepository = SharedPreferencesRepository.getInstance()
    val listType: Type = object : TypeToken<ArrayList<Entity>>() {}.type
    val favoriteEntities = Gson().fromJson<ArrayList<Entity>>(
        sharedPreferencesRepository.favoriteList.value, listType
    )
    if (favoriteEntities.isNullOrEmpty()) {
        return
    }

    sharedPreferencesRepository.favoriteList.removeStringPreference()

    sendEventRequest().setEvent(RemoveAllFavoritesEvent.builder().build())
        .asyncCall(object : Callback<SendEventResponse> {
            override fun onSuccess(response: SendEventResponse) {
                Log.d("RemoveAllFavoritesEvent", Gson().toJson(response))
            }

            override fun onFailure(e: Throwable) {
                e.printStackTrace()
            }
        })
}

fun DataConnectorClient.setHome(entity: Entity) {
    val eventBuilder = getHomeEventBuilder(entity)

    val setEvent = eventBuilder.setActionType(SetHomeEvent.ActionType.SET).build()
    val removeEvent = eventBuilder.setActionType(SetHomeEvent.ActionType.REMOVE).build()

    setAddress(
        entity,
        setEvent,
        removeEvent,
        true
    )
}

fun DataConnectorClient.removeHome() {
    val sharedPreferencesRepository = SharedPreferencesRepository.getInstance()
    val entity = Gson().fromJson(
        sharedPreferencesRepository.homeAddress.value,
        Entity::class.java
    )

    entity ?: return

    sharedPreferencesRepository.homeAddress.removeStringPreference()

    sendEventRequest().setEvent(
        getHomeEventBuilder(entity).setActionType(SetHomeEvent.ActionType.REMOVE).build()
    ).asyncCall(object : Callback<SendEventResponse> {
        override fun onSuccess(response: SendEventResponse) {
            Log.d("RemoveAddressEvent", Gson().toJson(response))
        }

        override fun onFailure(e: Throwable) {
            e.printStackTrace()
        }
    })
}

fun DataConnectorClient.setWork(entity: Entity) {
    val eventBuilder = getWorkEventBuilder(entity)

    val setEvent = eventBuilder.setActionType(SetWorkEvent.ActionType.SET).build()
    val removeEvent = eventBuilder.setActionType(SetWorkEvent.ActionType.REMOVE).build()

    setAddress(
        entity,
        setEvent,
        removeEvent,
        false
    )
}

fun DataConnectorClient.removeWork() {
    val sharedPreferencesRepository = SharedPreferencesRepository.getInstance()
    val entity = Gson().fromJson(
        sharedPreferencesRepository.workAddress.value,
        Entity::class.java
    )

    entity ?: return

    sharedPreferencesRepository.workAddress.removeStringPreference()

    sendEventRequest().setEvent(
        getWorkEventBuilder(entity).setActionType(SetWorkEvent.ActionType.REMOVE).build()
    ).asyncCall(object : Callback<SendEventResponse> {
        override fun onSuccess(response: SendEventResponse) {
            Log.d("RemoveAddressEvent", Gson().toJson(response))
        }

        override fun onFailure(e: Throwable) {
            e.printStackTrace()
        }
    })
}

private fun getHomeEventBuilder(entity: Entity): SetHomeEvent.Builder {
    val coords = entity.getCoord()
    val eventBuilder =
        SetHomeEvent.builder()
            .setLabel(entity.label ?: "").setEntityId(entity.id)
    if (coords != null) {
        eventBuilder.setLat(coords.latitude).setLon(coords.longitude)
    }

    return eventBuilder
}

private fun getWorkEventBuilder(entity: Entity): SetWorkEvent.Builder {
    val coords = entity.getCoord()
    val eventBuilder =
        SetWorkEvent.builder()
            .setLabel(entity.label ?: "").setEntityId(entity.id)
    if (coords != null) {
        eventBuilder.setLat(coords.latitude).setLon(coords.longitude)
    }

    return eventBuilder
}

private fun DataConnectorClient.setAddress(
    entity: Entity,
    setEvent: Event,
    removeEvent: Event,
    isHomeAddress: Boolean
) {
    val sharedPreferencesRepository = SharedPreferencesRepository.getInstance()
    val modify: Boolean
    val address = Gson().toJson(entity)
    if (isHomeAddress) {
        modify = sharedPreferencesRepository.isContainsHomeAddressKey()
        sharedPreferencesRepository.homeAddress.value = address
    } else {
        modify = sharedPreferencesRepository.isContainsWorkAddressKey()
        sharedPreferencesRepository.workAddress.value = address
    }

    if (modify) {
        sendEventRequest().setEvent(removeEvent).asyncCall(object : Callback<SendEventResponse> {
            override fun onSuccess(response: SendEventResponse) {
                Log.d("RemoveAddressEvent", Gson().toJson(response))
            }

            override fun onFailure(e: Throwable) {
                e.printStackTrace()
            }
        })
    }

    sendEventRequest().setEvent(setEvent).asyncCall(object : Callback<SendEventResponse> {
        override fun onSuccess(response: SendEventResponse) {
            Log.d("SetAddressEvent", Gson().toJson(response))
        }

        override fun onFailure(e: Throwable) {
            e.printStackTrace()
        }
    })
}

fun DataConnectorClient.entityClick(
    referenceId: String,
    entityId: String,
    displayMode: EntityActionEvent.DisplayMode
) {
    sendEventRequest().setEvent(
        EntityActionEvent.builder().setActionType(EntityActionEvent.ActionType.CLICK)
            .setReferenceId(referenceId).setEntityId(entityId).setDisplayMode(displayMode).build()
    ).asyncCall(object : Callback<SendEventResponse> {
        override fun onSuccess(response: SendEventResponse) {
            Log.d("EntityActionEvent.CLICK", Gson().toJson(response))
        }

        override fun onFailure(e: Throwable) {
            e.printStackTrace()
        }
    })
}

fun DataConnectorClient.entityCall(
    referenceId: String, entityId: String, displayMode: EntityActionEvent.DisplayMode
) {
    sendEventRequest().setEvent(
        EntityActionEvent.builder().setActionType(EntityActionEvent.ActionType.CALL)
            .setReferenceId(referenceId).setEntityId(entityId).setDisplayMode(displayMode).build()
    ).asyncCall(object : Callback<SendEventResponse> {
        override fun onSuccess(response: SendEventResponse) {
            Log.d("EntityActionEvent.CALL", Gson().toJson(response))
        }

        override fun onFailure(e: Throwable) {
            e.printStackTrace()
        }
    })
}

fun DataConnectorClient.entityCachedClick(
    entityId: String,
    source: EntityCacheActionEvent.SourceType
) {
    sendEventRequest().setEvent(
        EntityCacheActionEvent.builder().setActionType(EntityCacheActionEvent.ActionType.CLICK)
            .setEntityId(entityId).setSourceType(source).build()
    ).asyncCall(object : Callback<SendEventResponse> {
        override fun onSuccess(response: SendEventResponse) {
            Log.d("EntityCacheActionEvent.CLICK", Gson().toJson(response))
        }

        override fun onFailure(e: Throwable) {
            e.printStackTrace()
        }
    })
}

fun DataConnectorClient.entityCachedCall(
    entityId: String,
    source: EntityCacheActionEvent.SourceType
) {
    sendEventRequest().setEvent(
        EntityCacheActionEvent.builder().setActionType(EntityCacheActionEvent.ActionType.CALL)
            .setEntityId(entityId).setSourceType(source).build()
    ).asyncCall(object : Callback<SendEventResponse> {
        override fun onSuccess(response: SendEventResponse) {
            Log.d("EntityCacheActionEvent.CALL", Gson().toJson(response))
        }

        override fun onFailure(e: Throwable) {
            e.printStackTrace()
        }
    })
}

fun DataConnectorClient.gpsProbe(location: Location?) {
    location ?: return
    sendEventRequest().setEvent(
        GpsProbeEvent.builder()
            .setLat(location.latitude)
            .setLon(location.longitude)
            .setAltitude(location.altitude)
            .setSpeed(location.speed.toDouble())
            .setTimestamp(location.time)
            .build()
    ).asyncCall(object : Callback<SendEventResponse> {
        override fun onSuccess(response: SendEventResponse) {
            Log.d("GpsProbeEvent", Gson().toJson(response))
        }

        override fun onFailure(e: Throwable) {
            e.printStackTrace()
        }
    })
}