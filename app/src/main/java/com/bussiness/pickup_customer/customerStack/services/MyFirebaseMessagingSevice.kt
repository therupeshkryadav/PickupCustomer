package com.bussiness.pickup_customer.customerStack.services

import android.util.Log
import com.bussiness.pickup_customer.customerStack.CustomerCommon
import com.bussiness.pickup_customer.customerStack.utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingSevice() : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("TAG", "Refreshed token: $token")
        if (FirebaseAuth.getInstance().currentUser != null)
            UserUtils.updateToken(this, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("TAG", "Remote Message: $remoteMessage")
        val data= remoteMessage.data
        if(data != null)
        {
            CustomerCommon.showNotification(this, Random.nextInt(),
                data[CustomerCommon.NOTI_TITLE],
                data[CustomerCommon.NOTI_BODY],
                null)
        }
    }

}