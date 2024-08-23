package com.bussiness.pickup_customer.customerStack

import com.bussiness.pickup.customerStack.customerModel.CustomerInfoModel

object CustomerCommon {
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
    var currentUser: CustomerInfoModel?= null
    val CUSTOMER_INFO_REFERENCE: String="CustomersInfo"
    val CUSTOMER_LOCATION_REFERENCE: String = "CustomersLocation"
}