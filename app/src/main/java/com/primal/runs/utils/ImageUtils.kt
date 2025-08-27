package com.primal.runs.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatRatingBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.BindingAdapter
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.primal.runs.R
import com.primal.runs.data.model.DisplayItem
import com.primal.runs.data.model.LoginUser
import com.primal.runs.data.model.PreviousResult
import com.primal.runs.ui.dashboard.free_run.model.BadgesData
import com.primal.runs.ui.dashboard.home.model.GroupedData
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ImageUtils {


    fun navigateWithSlideAnimations(navController: NavController, destinationId: Int) {
        val navOptions =
            NavOptions.Builder().setEnterAnim(R.anim.slide_in_right) // Define enter animation
                .setExitAnim(R.anim.slide_out_left) // Define exit animation
                .setPopEnterAnim(R.anim.slide_in_left) // Define pop enter animation
                .setPopExitAnim(R.anim.slide_out_right) // Define pop exit animation
                .build()

        navController.navigate(destinationId, null, navOptions)
    }

    fun goActivity(context: Context, activity: Activity) {
        val intent = Intent(context, activity::class.java)
        startActivity(context, intent, null)
    }

    fun statusBarStyleWhite(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            activity.window.statusBarColor = Color.TRANSPARENT
        }
    }

    fun statusBarStyleBlack(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR // Ensures black text/icons
            activity.window.statusBarColor = Color.TRANSPARENT // Transparent status bar
        }
    }

    fun getStatusBarColor(activity: Activity, intColor: Int) {
        activity.window.statusBarColor = ContextCompat.getColor(activity, intColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.let { controller ->
                if (intColor == R.color.white) {
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    controller.setSystemBarsAppearance(
                        0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        } else {
            // Fallback for older Android versions using systemUiVisibility
            @Suppress("DEPRECATION") if (intColor == R.color.white) {
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                activity.window.decorView.systemUiVisibility = 0
            }
        }
    }

    fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and potentially store the token
            Log.d("FCM_TOKEN", "FCM Registration Token: $token")

            // TODO: Send the token to your app server.
            // You should send this token to your backend server to associate it
            // with the user's account for targeted messaging.
            // sendRegistrationToServer(token)
        })
    }

    inline fun <reified T> parseJson(json: String): T? {
        return try {
            val gson = Gson()
            gson.fromJson(json, T::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("printStackTrace", "parseJson: ${e.message} ")
            null
        }
    }

    fun hasPermissions(context: Context?, permissions: Array<String>?): Boolean {
        if (context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context, permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            /*Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.CAMERA,*/
            //Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            /*Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,*/
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

    val permissionsForLocationOny = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val storagePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.CAMERA,
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
        )
    }


    fun styleSystemBars(activity: Activity, color: Int) {
        activity.window.navigationBarColor = color
    }

    @BindingAdapter("setLocation")
    @JvmStatic
    fun setLocation(text: TextView, data: LoginUser?) {
        if (data != null) {
            if (data.lat != null || data.lng != null) {
                val addressName = getAddressName(text.context, data.lat!!, data.lng!!)
                if (addressName != null) {
                    text.text = addressName
                }
            }
        }

    }


    @BindingAdapter("loadIMage")
    @JvmStatic
    fun loadIMage(imageView: ImageView, data: String?) {
        if (data != null) {
            val data1 = data.replace("https://primalrunbucket.s3.us-east-1.amazonaws.com/", "")

            if (data1.isNullOrEmpty() || data1 == "null") {
                val initialsBitmap = generateInitialsBitmap("P")
                imageView.setImageBitmap(initialsBitmap)
            } else {
                Glide.with(imageView.context)
                    .load(data)
                    .placeholder(R.drawable.iv_dummy) // Placeholder while loading
                    .into(imageView)
            }
        }
    }

    @BindingAdapter("loadShapeableImage")
    @JvmStatic
    fun loadShapeableImage(imageView: ShapeableImageView, data: String?) {
        if (data != null) {
            Glide.with(imageView.context)
                .load(data)
                .placeholder(R.drawable.iv_dummy) // Placeholder while loading
                .error(R.drawable.iv_dummy) // Placeholder for errors
                // .skipMemoryCache(true) // Disable memory caching
                .diskCacheStrategy(DiskCacheStrategy.NONE) // Disable disk caching
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.iv_dummy) // Set a default image for null data
        }
    }

    @SuppressLint("SetTextI18n")
    @BindingAdapter("setData")
    @JvmStatic
    fun setData(textView: AppCompatTextView, data: GroupedData) {
        textView.text =
            "From ${data.minDistance}${data.measureUnits} to ${data.maxDistance}${data.measureUnits}"
    }

    @BindingAdapter("setCategoryName")
    @JvmStatic
    fun setCategoryName(textView: AppCompatTextView, categoryName: String?) {
        if (categoryName != null) {
            val formattedName: String = categoryName.replace("_", " ")
            textView.text = formattedName
        }

    }


    @BindingAdapter("showSelected")
    @JvmStatic
    fun showSelected(text: ShapeableImageView, data: BadgesData?) {
        if (data != null) {
            if (data.isSelected) {
                text.strokeWidth = 6f
            } else {
                text.strokeWidth = 0f
            }
        }
    }

    @BindingAdapter("showSelected")
    @JvmStatic
    fun showSelected(text: ShapeableImageView, isSelected: Boolean?) {
        if (isSelected != null) {
            if (isSelected) {
                text.strokeWidth = 6f
            } else {
                text.strokeWidth = 0f
            }
        }
    }

    @BindingAdapter("showAtThirdPosition")
    @JvmStatic
    fun showAtThirdPosition(view: View, position: Int) {
        try {
            // Show the view at every 3rd position
            if (position % 3 == 2) {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @BindingAdapter("setVisibility")
    @JvmStatic
    fun setVisibilityRateBar(view: AppCompatRatingBar, value: Boolean = true) {
        try {
            // Show the view at every 3rd position
            if (value) {
                view.visibility = View.VISIBLE
            } else {
                view.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    @BindingAdapter("showAtThirdPositionAds")
    @JvmStatic
    fun showAtThirdPositionAds(adManager: AdManager, position: Int) {
        try {
            if (adManager.adSize == null) {
                adManager.loadAd(object : AdManager.AdManagerCallback {
                    override fun onAdLoaded() {

                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {

                    }

                    override fun onAdClicked() {

                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun categorizeResultsByMonthCompat(jsonResponse: String): Map<String, List<JSONObject>> {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val resultsByMonth = mutableMapOf<String, MutableList<JSONObject>>()

        // Parse JSON response
        val dataObject = JSONObject(jsonResponse).getJSONObject("data")
        val previousResults = dataObject.getJSONArray("previousResults")

        // Get current month and year
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
        val currentYear = now.get(Calendar.YEAR)

        for (i in 0 until previousResults.length()) {
            val result = previousResults.getJSONObject(i)
            val createdAt = result.getString("createdAt")
            val createdDate = formatter.parse(createdAt) ?: continue

            val calendar = Calendar.getInstance().apply { time = createdDate }
            val monthName = when {
                calendar.get(Calendar.YEAR) == currentYear && calendar.get(Calendar.MONTH) + 1 == currentMonth -> "This Month"
                else -> calendar.getDisplayName(
                    Calendar.MONTH, Calendar.LONG, Locale.getDefault()
                ) // Get month name
            }

            // Add the result to the appropriate month group
            resultsByMonth.getOrPut(monthName) { mutableListOf() }.add(result)
        }

        return resultsByMonth
    }

    fun categorizeResultsByMonth(results: List<PreviousResult>): Map<String, List<PreviousResult>> {
        val categorizedResults = mutableMapOf<String, MutableList<PreviousResult>>()

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        results.forEach {
            val date = formatter.parse(it.createdAt)
            val calendar = Calendar.getInstance().apply { time = date }

            // Get the month and year for categorization
            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)

            // Format month name (e.g., "December")
            val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(date)

            // Determine if it's "THIS MONTH"
            val key = if (month == currentMonth && year == currentYear) {
                "THIS MONTH"
            } else {
                monthName // Exclude the year
            }

            // Add to the categorized map
            categorizedResults.getOrPut(key) { mutableListOf() }.add(it)
        }

        return categorizedResults
    }

    fun prepareDisplayItemsInOrder(categorizedResults: Map<String, List<PreviousResult>>): List<DisplayItem> {
        val displayItems = mutableListOf<DisplayItem>()

        // Sort the keys with "THIS MONTH" first, followed by other months in reverse chronological order
        val sortedKeys = categorizedResults.keys.sortedWith { key1, key2 ->
            when {
                key1 == "THIS MONTH" -> -1 // "THIS MONTH" always comes first
                key2 == "THIS MONTH" -> 1
                else -> {
                    // Compare month indices for reverse chronological order
                    val monthOrder = listOf(
                        "January",
                        "February",
                        "March",
                        "April",
                        "May",
                        "June",
                        "July",
                        "August",
                        "September",
                        "October",
                        "November",
                        "December"
                    )
                    val key1Index = monthOrder.indexOf(key1)
                    val key2Index = monthOrder.indexOf(key2)
                    key2Index - key1Index // Reverse chronological order
                }
            }
        }

        // Flatten the data
        sortedKeys.forEach { month ->
            displayItems.add(DisplayItem(isHeader = true, header = month))
            // Add the activities for that month
            categorizedResults[month]?.let { activities ->
                activities.forEach { activity ->
                    displayItems.add(DisplayItem(isHeader = false, activity = activity))
                }
            }
        }

        return displayItems
    }

    @BindingAdapter("formatDateAdapter")
    @JvmStatic
    fun formatDateAdapter(text: TextView, isoDate: String) {
        // Define the input format
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")

        // Define the output format
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        // Parse the input date and format it to the desired output
        val date: Date? = inputFormat.parse(isoDate)
        text.text = outputFormat.format(date)
    }

    fun formatDate(isoDate: String): String {
        // Define the input format
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")

        // Define the output format
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        // Parse the input date and format it to the desired output
        val date: Date = inputFormat.parse(isoDate)
        return outputFormat.format(date)
    }

    fun formatDuration(totalDuration: Int): String {
        val hours = totalDuration / 60
        val minutes = totalDuration % 60
        return "${hours}H ${minutes}M"
    }

    @BindingAdapter("setColorMatrix")
    @JvmStatic
    fun setColorMatrix(imageView: ShapeableImageView, lock: Boolean) {
        Log.d("setColorMatrix", "setColorMatrix: $lock")
        if (!lock) {
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)

            val filter = ColorMatrixColorFilter(colorMatrix)
            imageView.colorFilter = filter
        } else {
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(1f)

            val filter = ColorMatrixColorFilter(colorMatrix)
            imageView.colorFilter = filter
        }
    }

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun openLocationSettings(activity: AppCompatActivity) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        activity.startActivity(intent)
    }


    fun generateInitialsBitmap(name: String): Bitmap {


        val initials = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()

        Log.d("initials", "generateInitialsBitmap: name $name, initials $initials")

        val size = 200
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK // Change to your preferred background color
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Draw Circle Background
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw Initials Text
        paint.apply {
            // color = Color.WHITE
            color = Color.parseColor("#D2FF22")
            textSize = size / 2f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val textBounds = Rect()
        paint.getTextBounds(initials, 0, initials.length, textBounds)
        val xPos = size / 2f
        val yPos = (size / 2f - textBounds.exactCenterY())

        canvas.drawText(initials, xPos, yPos, paint)

        return bitmap
    }

    fun unitsOfMeasure(measure: Int): String {
        return if (measure == 1) {
            "/KM"
        } else {
            "/MILES"
        }
    }

    fun measure(measure: Int): String {
        return if (measure == 1) {
            " KM"
        } else {
            " MILES"
        }
    }

    fun paceToSpeed(
        paceMinutes: Int,
        paceSeconds: Int,
        isPacePerMile: Boolean = false
    ): Double {
        val paceInSeconds = (paceMinutes * 60) + paceSeconds
        val distanceInMeters = if (isPacePerMile) 1609.34 else 1000.0
        return distanceInMeters / paceInSeconds // Speed in m/s
    }

    fun paceToSpeed(pace: Double?, isPacePerMile: Boolean = false): Double {
        val minutes = pace!!.toInt()
        val secondsFraction = pace - minutes
        val totalSeconds = (minutes * 60) + (secondsFraction * 60)

        val distanceInMeters = if (isPacePerMile) 1609.34 else 1000.0
        return distanceInMeters / totalSeconds // Speed in m/s
    }

    fun convertPaceToSeconds(pace: String): Int {
        try {
            val parts = pace.split(":")
            val minutes = parts[0].toIntOrNull() ?: 0
            val seconds = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return (minutes * 60) + seconds
        } catch (ex: Exception) {
            ex.printStackTrace()
            return 0
        }

    }

}

fun getAddressName(context: Context, latitude: Double, longitude: Double): String? {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude, longitude, 1) // Get 1 result
        if (!addresses.isNullOrEmpty()) {
            return addresses[0].locality
                ?: addresses[0].getAddressLine(0) // Locality or fallback to full address
        }
    } catch (e: Exception) {
        e.printStackTrace() // Handle exceptions, e.g., IO issues or invalid coordinates
    }
    return null
}

enum class BadgeType(val type: Int) {
    ELEPHANT(1), BULL(2), GORILLA(3), BEAR(4), DEER(5), TIGER(6), WOLF(7), RHINO(8), LION(9), RAPTOR(
        10
    );

    companion object {
        fun fromType(type: Int): BadgeType? {
            return values().find { it.type == type }
        }
    }
}





