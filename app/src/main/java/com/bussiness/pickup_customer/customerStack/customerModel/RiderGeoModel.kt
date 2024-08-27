package com.bussiness.pickup_customer.customerStack.customerModel

import com.firebase.geofire.GeoLocation

class RiderGeoModel {
    var key:String?=null
    var geoLocation:GeoLocation?=null
    var riderInfoModel:RiderInfoModel?=null

    constructor(key:String?,geoLocation: GeoLocation?)
    {
        this.key = key
        this.geoLocation = geoLocation!!
    }
}