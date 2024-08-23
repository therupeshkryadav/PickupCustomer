package com.bussiness.pickup_customer.customerStack.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bussiness.pickup_customer.R
import com.bussiness.pickup_customer.customerStack.CustomerCommon
import com.bussiness.pickup_customer.customerStack.CustomerLoginActivity
import com.bussiness.pickup_customer.databinding.FragmentHomeCustomerBinding
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var _binding: FragmentHomeCustomerBinding? = null
    private lateinit var mapFragment:SupportMapFragment

    //Location
    private lateinit var locationRequest:LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //Online System
    private lateinit var onlineRef:DatabaseReference
    private lateinit var currentUserRef:DatabaseReference
    private lateinit var driverLocationRef:DatabaseReference
    private lateinit var geofire: GeoFire

    private val onlineEventListener = object:ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists())
                currentUserRef.onDisconnect().removeValue()
        }

        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(mapFragment.requireView(),error.message,Snackbar.LENGTH_LONG).show()
        }

    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        FirebaseAuth.getInstance().currentUser?.let { user ->
            geofire.removeLocation(user.uid)
        }
        onlineRef.removeEventListener(onlineEventListener)
    }

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineEventListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeCustomerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        init()

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return root
    }

    @SuppressLint("VisibleForTests")
    private fun init() {

        onlineRef = CustomerLoginActivity.database.getReference().child(".info/connected")
        driverLocationRef = CustomerLoginActivity.database.getReference(CustomerCommon.CUSTOMER_LOCATION_REFERENCE)
        currentUserRef = driverLocationRef.child(
            FirebaseAuth.getInstance().currentUser!!.uid
        )

        geofire= GeoFire(driverLocationRef)

        registerOnlineSystem()

        locationRequest = LocationRequest()
        locationRequest.setPriority(PRIORITY_HIGH_ACCURACY)
        locationRequest.setFastestInterval(3000)
        locationRequest.interval = 5000
        locationRequest.setSmallestDisplacement(10f)


        locationCallback = object : LocationCallback(){
            // ctrl + O
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val newPos = LatLng(
                    locationResult.lastLocation!!.latitude,
                    locationResult.lastLocation!!.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))

                // Update location

                geofire.setLocation(
                    CustomerLoginActivity.firebaseAuth.currentUser!!.uid,
                    GeoLocation(locationResult.lastLocation!!.latitude,
                        locationResult.lastLocation!!.longitude),
                ){ key:String?, error:DatabaseError? ->
                    if(error != null)
                        Snackbar.make(mapFragment.requireView(),error.message,Snackbar.LENGTH_LONG).show()
                    else
                        Snackbar.make(mapFragment.requireView(),"You're online!",Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,Looper.myLooper())

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //Request permission
        Dexter.withContext(context)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener{
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {

                    //Enable Button first
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return
                    }
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationClickListener {
                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener{e ->
                                Toast.makeText(
                                    context!!,
                                    e.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.addOnSuccessListener { location ->
                                val userLatLng = LatLng(location.latitude,location.longitude)
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng,18f))
                            }
                        true
                    }

                    //layout
                    val view = mapFragment.requireView()
                        .findViewById<View>("1".toInt())
                        .parent as View
                    val locationButton = view.findViewById<View>("2".toInt())
                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_TOP,0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE)
                    params.bottomMargin = 50


                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(context!!,"Permission "+p0!!.permissionName+" was denied",Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    TODO("Not yet implemented")
                }

            }).check()

        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),
                R.raw.uber_maps_style))
            if (!success)
                Log.e("EDMT_ERROR","Style parsing error")
        }catch (e: Resources.NotFoundException)
        {
            Log.e("EDMT_ERROR", e.message.toString())
        }


    }
}