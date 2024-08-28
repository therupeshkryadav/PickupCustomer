package com.bussiness.pickup_customer.customerStack.callback

import com.bussiness.pickup_customer.customerStack.customerModel.RiderGeoModel

interface FirebaseRiderInfoListener {
    fun onRiderInfoLoadSuccess(riderGeoModel: RiderGeoModel?)
}