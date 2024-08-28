package com.bussiness.pickup_customer.customerStack.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bussiness.pickup_customer.R
import com.bussiness.pickup_customer.customerStack.CustomerCommon
import com.bussiness.pickup_customer.customerStack.callback.FirebaseRiderInfoListener
import com.bussiness.pickup_customer.customerStack.callback.FirebaseFailedListener
import com.bussiness.pickup_customer.customerStack.customerModel.GeoQueryModel
import com.bussiness.pickup_customer.customerStack.customerModel.RiderGeoModel
import com.bussiness.pickup_customer.customerStack.customerModel.RiderInfoModel
import com.bussiness.pickup_customer.databinding.FragmentHomeCustomerBinding
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryEventListener
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.IOException
import java.util.Locale

class HomeFragment : Fragment(), OnMapReadyCallback, FirebaseRiderInfoListener {

    private lateinit var mMap: GoogleMap
    private var _binding: FragmentHomeCustomerBinding? = null
    private lateinit var mapFragment:SupportMapFragment

    //Location
    private lateinit var locationRequest:LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //Load Riders
    var distance = 1.0
    val LIMIT_RANGE = 10.0
    var previousLocation: Location?= null
    var currentLocation:Location?= null

    var firstTime = true

    //Listener
    var iFirebaseRiderInfoListener: FirebaseRiderInfoListener?=null
    var iFirebaseFailedListener: FirebaseFailedListener?=null

    var cityName = ""

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
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
        iFirebaseRiderInfoListener= this

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

                //If user has change location,calculate and load driver again
                if (firstTime)
                {
                    previousLocation=locationResult.lastLocation
                    currentLocation=locationResult.lastLocation

                    firstTime=false
                }else{
                    previousLocation=currentLocation
                    currentLocation=locationResult.lastLocation
                }

                if(previousLocation!!.distanceTo(currentLocation!!)/1000 <= LIMIT_RANGE)
                    loadAvailableRiders()
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
//          Snackbar.make(requireView(),getString(R.string.permission_require),Snackbar.LENGTH_SHORT).show()
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,Looper.myLooper())

        loadAvailableRiders()

    }

    private fun loadAvailableRiders() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(requireView(),getString(R.string.permission_require),Snackbar.LENGTH_SHORT).show()
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnFailureListener { e->
                Snackbar.make(requireView(),e.message!!,Snackbar.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { location->

                //Load all riders in city
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                var addressList : List<Address> = ArrayList()
                try {
                    addressList = geocoder.getFromLocation(location.latitude,location.longitude,1)!!
                    cityName= addressList[0].locality

                    //Query
                    val rider_location_ref = FirebaseDatabase.getInstance()
                        .getReference(CustomerCommon.RIDER_LOCATION_REFERENCE)
                        .child(cityName)
                    val gf=GeoFire(rider_location_ref)
                    val geoQuery = gf.queryAtLocation(GeoLocation(location.latitude,location.longitude),distance)
                    geoQuery.removeAllListeners()

                    geoQuery.addGeoQueryEventListener(object:GeoQueryEventListener{
                        override fun onGeoQueryReady() {
                            if (distance <= LIMIT_RANGE)
                            {
                                distance++
                                loadAvailableRiders()
                            }
                            else{
                                distance = 0.0
                                addRiderMarker()
                            }
                        }
                        override fun onKeyEntered(key: String?, location: GeoLocation?) {
                            Log.d("RiderMarker", "Key entered: $key at location: $location")
                            if (key != null && location != null) {
                                CustomerCommon.ridersFound.add(RiderGeoModel(key, location))
                                Log.d("RiderMarker", "${CustomerCommon.ridersFound.add(RiderGeoModel(key, location))}")
                            }
                        }

                        override fun onKeyMoved(key: String?, location: GeoLocation?) {

                        }

                        override fun onKeyExited(key: String?) {

                        }

                        override fun onGeoQueryError(error: DatabaseError?) {
                            Snackbar.make(requireView(),error!!.message,Snackbar.LENGTH_SHORT).show()
                        }

                    })

                    rider_location_ref.addChildEventListener(object:ChildEventListener{

                        override fun onCancelled(error: DatabaseError) {
                            Snackbar.make(requireView(),error.message,Snackbar.LENGTH_SHORT).show()
                        }

                        override fun onChildMoved(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {

                        }

                        override fun onChildChanged(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {

                        }

                        override fun onChildAdded(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            //Have new rider
                            val geoQueryModel = snapshot.getValue(GeoQueryModel::class.java)
                            val geoLocation = GeoLocation(geoQueryModel!!.l!![0], geoQueryModel!!.l!![1])
                            val riderGeoModel = RiderGeoModel(snapshot.key,geoLocation)
                            val newRiderLocation = Location("")
                            newRiderLocation.latitude = geoLocation.latitude
                            newRiderLocation.longitude = geoLocation.longitude
                            val newDistance= location.distanceTo(newRiderLocation)/1000 //in km
                            if(newDistance <= LIMIT_RANGE)
                                findRiderByKey(riderGeoModel)
                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {

                        }

                    })

                }catch (e: IOException)
                {
                    Snackbar.make(requireView(),getString(R.string.permission_require),Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    @SuppressLint("CheckResult")
    private fun addRiderMarker() {
        Log.d("RiderMarker", "Checking riders. Size: ${CustomerCommon.ridersFound.size}")
        if (CustomerCommon.ridersFound.size > 0) {
            Observable.fromIterable(CustomerCommon.ridersFound)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { riderGeoModel: RiderGeoModel? ->
                        Log.d("RiderMarker", "Processing rider: $riderGeoModel")
                        findRiderByKey(riderGeoModel)
                    },
                    { t: Throwable ->
                        Log.e("RiderMarker", "Error: ${t.message}")
                        Snackbar.make(requireView(), t.message!!, Snackbar.LENGTH_SHORT).show()
                    }
                )
        } else {
            Snackbar.make(requireView(), getString(R.string.rider_not_found), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun findRiderByKey(riderGeoModel: RiderGeoModel?){
       FirebaseDatabase.getInstance()
           .getReference(CustomerCommon.RIDER_INFO_REFERENCE)
           .child(riderGeoModel!!.key!!)
           .addListenerForSingleValueEvent(object:ValueEventListener{

               override fun onCancelled(error: DatabaseError) {
                   iFirebaseFailedListener!!.onFirebaseFailed(error.message)
               }

               override fun onDataChange(snapshot: DataSnapshot) {
                   if(snapshot.hasChildren())
                   {
                       riderGeoModel.riderInfoModel= (snapshot.getValue(RiderInfoModel::class.java))
                      Log.d("RiderMarker","${riderGeoModel.riderInfoModel}")
                       iFirebaseRiderInfoListener!!.onRiderInfoLoadSuccess(riderGeoModel)
                   }
                   else
                   {
                       iFirebaseFailedListener?.onFirebaseFailed(getString(R.string.key_not_found)+riderGeoModel.key)
                   }
               }

           })
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
                                Snackbar.make(
                                    requireView(),
                                    e.message!!,
                                    Snackbar.LENGTH_LONG
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
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE)
                    params.bottomMargin = 250 // Move to see zoom control
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    TODO("Not yet implemented")
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Snackbar.make(requireView(),p0!!.permissionName+" needed for running the app",
                        Snackbar.LENGTH_LONG).show()
                }

            }).check()

        //Enable Zoom
        mMap.uiSettings.isZoomControlsEnabled = true


        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),
                R.raw.uber_maps_style))
            if (!success)
                Snackbar.make(requireView(),"Load map styles failed",
                    Snackbar.LENGTH_LONG).show()
        }catch (e: Exception)
        {
            Snackbar.make(requireView(),""+e.message,
                Snackbar.LENGTH_LONG).show()
        }


    }

    override fun onRiderInfoLoadSuccess(riderGeoModel: RiderGeoModel?) {
        //If already have marker with this key, doesn't set it again
        if(!CustomerCommon.markerList.containsKey(riderGeoModel!!.key))
            mMap.addMarker(MarkerOptions()
                .position(LatLng(riderGeoModel!!.geoLocation!!.latitude,riderGeoModel!!.geoLocation!!.longitude))
                .flat(true)
                .title(CustomerCommon.buildName(riderGeoModel!!.riderInfoModel!!.firstName,riderGeoModel!!.riderInfoModel!!.lastName))
                .snippet(riderGeoModel.riderInfoModel!!.phoneNumber)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)))?.let {
                CustomerCommon.markerList.put(riderGeoModel!!.key!!,
                    it
                )
            }

        if (!TextUtils.isEmpty(cityName))
        {
            val riderLocation = FirebaseDatabase.getInstance()
                .getReference(CustomerCommon.RIDER_LOCATION_REFERENCE)
                .child(cityName)
                .child(riderGeoModel!!.key!!)
            riderLocation.addValueEventListener(object : ValueEventListener{

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(requireView(), error.message,Snackbar.LENGTH_SHORT).show()
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if(!snapshot.hasChildren())
                    {
                        if(CustomerCommon.markerList.get(riderGeoModel!!.key!!) != null)
                        {
                           val marker = CustomerCommon.markerList.get(riderGeoModel!!.key!!)
                            marker!!.remove() //Remove marker from map
                            CustomerCommon.markerList.remove(riderGeoModel!!.key!!) //Remove marker information
                            riderLocation.removeEventListener(this)
                        }
                    }
                }

            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}