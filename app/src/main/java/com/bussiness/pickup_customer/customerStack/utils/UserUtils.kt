package com.bussiness.pickup_customer.customerStack.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.bussiness.pickup_customer.customerStack.CustomerCommon
import com.bussiness.pickup_customer.customerStack.customerModel.TokenInfoModel
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

    fun updateToken(context: Context, token: String) {
        val tokenModel= TokenInfoModel()
        tokenModel.token = token

        FirebaseDatabase.getInstance()
            .getReference(CustomerCommon.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(token)
            .addOnFailureListener {e-> Toast.makeText(context,e.message, Toast.LENGTH_LONG).show() }
            .addOnSuccessListener {  }
    }

}