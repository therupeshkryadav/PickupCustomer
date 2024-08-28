package com.bussiness.pickup_customer.customerStack.utils

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bussiness.pickup_customer.customerStack.CustomerCommon
import com.bussiness.pickup_customer.customerStack.customerModel.TokenInfoModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object UserUtils {

    fun updateUser(
        view: View?,
        updateData: Map<String, Any>
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            FirebaseDatabase.getInstance()
                .getReference("Users").child(CustomerCommon.CUSTOMER_INFO_REFERENCE)
                .child(currentUser.uid)
                .updateChildren(updateData)
                .addOnFailureListener { e ->
                    view?.let {
                        Snackbar.make(it, e.message ?: "Failed to update user information.", Snackbar.LENGTH_LONG).show()
                    } ?: run {
                        // Fallback logging or handling if view is null
                        Log.e("UserUtils", "Update failed: ${e.message}")
                    }
                }
                .addOnSuccessListener {
                    view?.let {
                        Snackbar.make(it, "Information updated successfully!", Snackbar.LENGTH_LONG).show()
                    } ?: run {
                        // Fallback handling if view is null
                        Log.i("UserUtils", "User information updated successfully.")
                    }
                }
        } else {
            view?.let {
                Snackbar.make(it, "User is not authenticated.", Snackbar.LENGTH_LONG).show()
            } ?: run {
                // Fallback handling if view is null
                Log.e("UserUtils", "User not authenticated.")
            }
        }
    }


    fun updateToken(context: Context, token: String) {
        val tokenModel= TokenInfoModel()
        tokenModel.token = token

        FirebaseDatabase.getInstance()
            .getReference(CustomerCommon.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(tokenModel)
            .addOnFailureListener {e-> Toast.makeText(context,e.message, Toast.LENGTH_LONG).show() }
            .addOnSuccessListener {  }
    }

}