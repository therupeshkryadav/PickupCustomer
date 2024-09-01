@file:Suppress("UNREACHABLE_CODE", "CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST")

package com.bussiness.pickup_customer.customerStack

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.bussiness.pickup_customer.R
import com.bussiness.pickup_customer.customerStack.customerModel.AnimationModel
import com.bussiness.pickup_customer.customerStack.customerModel.CustomerInfoModel
import com.bussiness.pickup_customer.customerStack.customerModel.RiderGeoModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.atan


object CustomerCommon {
    val ridersSubscribe: MutableMap<String,AnimationModel> = HashMap<String, AnimationModel>()
    val markerList: MutableMap<String, Marker> =HashMap<String, Marker>()
    val RIDER_INFO_REFERENCE: String= "RiderInfo"
    val ridersFound: MutableSet<RiderGeoModel> =HashSet<RiderGeoModel>()
    val RIDER_LOCATION_REFERENCE: String= "RidersLocation"
    val NOTI_BODY: String = "body"
    val NOTI_TITLE: String = "title"

    val TOKEN_REFERENCE: String = "Token"

    var currentUser: CustomerInfoModel?= null
    val CUSTOMER_INFO_REFERENCE: String="CustomersInfo"

    // Use safe calls to avoid NullPointerException
    fun buildWelcomeMessage(): String {
        val firstName = currentUser!!.firstName ?: "User"
        val lastName = currentUser!!.lastName ?: ""
        return StringBuilder("Welcome, ")
            .append(firstName)
            .append(" ")
            .append(lastName)
            .toString()
    }

    fun showNotification(context: Context, id: Int, title: String?, body: String?, intent: Intent?) {
        var pendingIntent: PendingIntent?= null
        if(intent != null)
            pendingIntent= PendingIntent.getActivity(context,id,intent!!, PendingIntent.FLAG_UPDATE_CURRENT)
        val NOTIFICATION_CHANNEL_ID = "Pickup"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,"Pickup",
                NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = "Pickup"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000)
            notificationChannel.enableVibration(true)

            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
        builder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.noti_pickup)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.drawable.noti_pickup))

        if(pendingIntent != null)
            builder.setContentIntent(pendingIntent!!)
        val notification = builder.build()
        notificationManager.notify(id,notification)

    }

    fun buildName(firstName: String, lastName: String): String? {
        return java.lang.StringBuilder(firstName).append(" ").append(lastName).toString()
    }

    fun decodePoly(encoded: String): java.util.ArrayList<LatLng?> {
        val poly = ArrayList<LatLng?>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
            lng += dlng

            val p = LatLng(
                ((lat.toDouble() / 1E5)),
                ((lng.toDouble() / 1E5))
            )
            poly.add(p as Nothing)
        }
        return poly
    }

    fun getBearing(begin: LatLng, end: LatLng): Float {
        //You can copy this function by link at description
        val lat = abs(begin.latitude - end.latitude)
        val lng = abs(begin.longitude - end.longitude)

        if (begin.latitude < end.latitude && begin.longitude < end.longitude) return Math.toDegrees(
            atan(lng / lat)
        )
            .toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude) return ((90 - Math.toDegrees(
            atan(lng / lat)
        )) + 90).toFloat()
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude) return (Math.toDegrees(
            atan(lng / lat)
        ) + 180).toFloat()
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude) return ((90 - Math.toDegrees(
            atan(lng / lat)
        )) + 270).toFloat()
        return (-1).toFloat()
    }

    fun setWelcomeMessage(txtWelcome: TextView) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if(hour >= 1 && hour <= 12)
            txtWelcome.setText(java.lang.StringBuilder("Good morning."))
        else if(hour > 12 && hour <= 17)
            txtWelcome.setText(java.lang.StringBuilder("Good afternoon."))
        else
            txtWelcome.setText(java.lang.StringBuilder("Good evening."))
    }

}