package com.bussiness.pickup_customer.customerStack.Callback

import com.bussiness.pickup_customer.customerStack.customerModel.RiderGeoModel

interface FirebaseRiderInfoListener {
    fun onRiderInfoLoadSuccess(riderGeoModel: RiderGeoModel?)
}