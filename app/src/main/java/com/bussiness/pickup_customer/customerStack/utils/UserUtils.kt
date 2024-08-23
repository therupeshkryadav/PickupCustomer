package com.bussiness.pickup_customer.customerStack.utils

import android.view.View
import com.bussiness.pickup_customer.customerStack.CustomerCommon
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object UserUtils {

    fun updateUser(
        view:View?,
        updateData:Map<String,Any>
    ){
        FirebaseDatabase.getInstance()
            .getReference(CustomerCommon.CUSTOMER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener{e->
                Snackbar.make(view!!,e.message!!,Snackbar.LENGTH_LONG).show()
            }.addOnSuccessListener {
                Snackbar.make(view!!,"Update Information Successfully!!",Snackbar.LENGTH_LONG).show()
            }
    }
}