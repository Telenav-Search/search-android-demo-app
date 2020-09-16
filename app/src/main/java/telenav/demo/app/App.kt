package telenav.demo.app

import android.app.Application
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import com.telenav.sdk.core.Locale
import com.telenav.sdk.core.SDKOptions
import com.telenav.sdk.entity.api.EntityService


class App : Application() {

    override fun onCreate() {
        super.onCreate()
        EntityService.initialize(
            SDKOptions.builder()
                .setApiKey("7bd512e0-16bc-4a45-9bc9-09377ee8a913")
                .setApiSecret("89e872bc-1529-4c9f-857c-c32febbf7f5a")
                .setCloudEndPoint("https://restapistage.telenav.com")
                .setLocale(Locale.EN_US)
                .build()
        )
    }
}

fun Context.convertNumberToDistance(dist: Double): String {
    val km = dist / 1000.0;

    val iso = resources.configuration.locale.getISO3Country()
    return if (iso.equals("usa", true) || iso.equals("mmr", true)) {
        String.format("%.1f mi", km / 1.609)
    } else {
        String.format("%.1f km", km)
    }
}

fun View.expand(koef: Int = 1) {
    val matchParentMeasureSpec: Int =
        View.MeasureSpec.makeMeasureSpec((parent as View).width, View.MeasureSpec.EXACTLY)
    val wrapContentMeasureSpec: Int =
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    measure(matchParentMeasureSpec, wrapContentMeasureSpec)
    val targetHeight: Int = measuredHeight / koef

    layoutParams.height = 1
    visibility = View.VISIBLE
    val a: Animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            layoutParams.height =
                if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
            requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    a.duration = (targetHeight / context.resources.displayMetrics.density / 3).toLong()
    startAnimation(a)
}

fun View.collapse() {
    val initialHeight = measuredHeight
    val a: Animation = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
            if (interpolatedTime == 1f) {
                visibility = View.GONE
            } else {
                layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                requestLayout()
            }
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    a.duration = (initialHeight / context.resources.displayMetrics.density / 3).toLong()
    startAnimation(a)
}

fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
