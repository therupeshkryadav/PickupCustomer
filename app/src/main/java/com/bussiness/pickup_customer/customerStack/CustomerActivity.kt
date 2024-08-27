package com.bussiness.pickup_customer.customerStack

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.bussiness.pickup.customerStack.customerModel.CustomerInfoModel
import com.bussiness.pickup_customer.ChoiceActivity
import com.bussiness.pickup_customer.R
import com.bussiness.pickup_customer.customerStack.utils.UserUtils
import com.bussiness.pickup_customer.databinding.ActivityCustomerBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class CustomerActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityCustomerBinding
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var img_avatar: ImageView
    private lateinit var waitingDialog: AlertDialog
    private lateinit var storageReference: StorageReference
    private var imageUri: Uri? = null
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCustomerBinding.inflate(layoutInflater)
        // Remove the title bar
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        setSupportActionBar(binding.appBarNavigation.toolbar)

        drawerLayout = binding.drawerLayout
        navView = binding.navView

        // Ensure the view is fully created before accessing NavController
        binding.root.post {
            try {
                navController = findNavController(R.id.nav_host_fragment_content_customer)
                appBarConfiguration = AppBarConfiguration(
                    setOf(R.id.nav_home), drawerLayout
                )
                setupActionBarWithNavController(navController, appBarConfiguration)
                navView.setupWithNavController(navController)
                init()
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle or log the exception
            }
        }
    }


    private fun init() {
        storageReference = FirebaseStorage.getInstance().getReference()

        waitingDialog = AlertDialog.Builder(this)
            .setMessage("Waiting...")
            .setCancelable(false).create()

        navView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.nav_sign_out) {
                showSignOutDialog()
            }
            true
        }

        val headerView = navView.getHeaderView(0)
        val txt_name = headerView.findViewById<View>(R.id.txt_name) as TextView
        val txt_phone = headerView.findViewById<View>(R.id.txt_phone) as TextView
        img_avatar = headerView.findViewById<View>(R.id.img_avatar) as ImageView

        var customerInfoReference =
            FirebaseDatabase.getInstance().getReference("Users").child(CustomerCommon.CUSTOMER_INFO_REFERENCE)
        customerInfoReference
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val model = snapshot.getValue(CustomerInfoModel::class.java)
                        println("FIREBASE_USER => $model")
                        //setting up the ui
                        txt_name.text = model?.firstName
                        txt_phone.text = model?.phoneNumber

                        CustomerCommon.currentUser?.avatar?.takeIf { it.isNotEmpty() }?.let {
                            Glide.with(this@CustomerActivity)
                                .load(model?.avatar)
                                .into(img_avatar)
                        }

                        img_avatar.setOnClickListener {
                            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "image/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            startActivityForResult(Intent.createChooser(intent, "SELECT PICTURES"), PICK_IMAGE_REQUEST)
                        }
                    } else {
                        println("FIREBASE_USER => user not exist")
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                imageUri = uri
                showDialogUpload()
            }
        }
    }

    private fun showSignOutDialog() {
        val builder = AlertDialog.Builder(this@CustomerActivity)
        builder.setTitle("Sign out")
            .setMessage("Do you really want to sign out?")
            .setNegativeButton("CANCEL") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setPositiveButton("SIGN OUT") { dialogInterface, _ ->
                firebaseAuth.signOut()
                // Use the correct context for starting the activity
                val intent = Intent(this, ChoiceActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Ensure the current activity is finished
            }
            .setCancelable(false)

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(android.R.color.holo_red_dark))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(resources.getColor(R.color.yellow))
        }
        dialog.show()
    }

    private fun showDialogUpload() {
        val builder = AlertDialog.Builder(this@CustomerActivity)
        builder.setTitle("Change Avatar")
            .setMessage("Do you really want to change Avatar?")
            .setNegativeButton("CANCEL") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setPositiveButton("CHANGE") { dialogInterface, _ ->
                imageUri?.let { uri ->
                    waitingDialog.show()
                    val avatarFolder = storageReference.child("avatars/${firebaseAuth.currentUser!!.uid}")

                    avatarFolder.putFile(uri)
                        .addOnFailureListener { e ->
                            Snackbar.make(drawerLayout, e.message!!, Snackbar.LENGTH_LONG).show()
                            waitingDialog.dismiss()
                        }
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                avatarFolder.downloadUrl.addOnSuccessListener { downloadUri ->
                                    val updateData = hashMapOf("avatar" to downloadUri.toString())
                                    UserUtils.updateUser(drawerLayout, updateData)

                                    // Update the ImageView with the new avatar URL
                                    Glide.with(this)
                                        .load(downloadUri)
                                        .into(img_avatar)
                                }
                            }
                            waitingDialog.dismiss()
                        }
                        .addOnProgressListener { taskSnapshot ->
                            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                            waitingDialog.setMessage("Uploading: ${progress.toInt()}%")
                        }
                }
            }
            .setCancelable(false)

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(android.R.color.holo_red_dark))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(resources.getColor(R.color.yellow))
        }
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.customer, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object {
        const val PICK_IMAGE_REQUEST = 7272
    }
}
