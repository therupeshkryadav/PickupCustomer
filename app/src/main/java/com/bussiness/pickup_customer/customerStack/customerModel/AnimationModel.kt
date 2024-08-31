package com.bussiness.pickup_customer.customerStack.customerModel

import android.os.Handler
import com.google.android.gms.maps.model.LatLng

class AnimationModel(var isRun:Boolean, var geoQueryModel:GeoQueryModel)
{
    //Moving Marker
    var polylineList: java.util.ArrayList<LatLng?>? = null
    var handler: Handler
    var index: Int = 0
    var next: Int = 0
    var v: Float = 0.0f
    var lat: Double = 0.0
    var lng: Double = 0.0
    var start: LatLng? = null
    var end: LatLng? = null

    init {
        handler = Handler()

    }
}