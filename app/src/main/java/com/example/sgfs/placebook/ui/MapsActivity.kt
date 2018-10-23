package com.example.sgfs.placebook.ui

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.view.WindowManager
import android.widget.ProgressBar
import com.example.sgfs.placebook.R
import com.example.sgfs.placebook.R.id.add
import com.example.sgfs.placebook.R.id.bookmarkRecyclerView
import com.example.sgfs.placebook.adapter.BookmarkInfoWindowAdapter
import com.example.sgfs.placebook.adapter.BookmarkListAdapter
import com.example.sgfs.placebook.viewmodel.MapsViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.PlacePhotoMetadata
import com.google.android.gms.location.places.Places
import com.google.android.gms.location.places.ui.PlaceAutocomplete

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.drawer_view_maps.*
import kotlinx.android.synthetic.main.main_view_maps.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch

class MapsActivity() : AppCompatActivity(), OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        //failed
    }

    private val USER_LOCATION_STR = "User Location"

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var mapsViewModel: MapsViewModel
    private lateinit var bookmarkListAdapter: BookmarkListAdapter
    private var markers = HashMap<Long, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationClient()
        setupToolbar()
        setupGoogleClient()
        setupNavigationDrawer()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMapListeners()
        setupViewModel()
        getCurrentLocation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            AUTOCOMPLETE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val place = PlaceAutocomplete.getPlace(this, data)

                    val location = Location("")
                    location.latitude = place.latLng.latitude
                    location.longitude = place.latLng.longitude
                    updateMapToLocation(location)
                    showProgress()
                    displayPoiGetPhotoMetaDataStep(place)
                }
            }
        }
    }

    fun moveToBookmark(bookmark: MapsViewModel.SavedBookmarkView) {
        drawerLayout.closeDrawer(drawerView)
        val marker = markers[bookmark.id]
        marker?.showInfoWindow()
        val location = Location("")
        location.latitude = bookmark.location.latitude
        location.longitude = bookmark.location.longitude
        updateMapToLocation(location)
    }

    private fun showProgress() {
        progressBar.visibility = ProgressBar.VISIBLE
        disableUserInteraction()
    }

    private fun hideProgress() {
        progressBar.visibility = ProgressBar.GONE
        enableUserInteraction()
    }

    private fun disableUserInteraction() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun enableUserInteraction() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun newBookmark(latLng: LatLng) {
        launch(CommonPool) {
            val addBookmarkFromPlace = mapsViewModel.addBookmarkFromPlace(latLng)
            addBookmarkFromPlace?.let {
                startBookmarkDetails(it)
            }
        }
    }

    private fun searchAtCurrentLocation() {
        val bounds = mMap.projection.visibleRegion.latLngBounds

        try {
            val intent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .setBoundsBias(bounds)
                    .build(this)

            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        }
    }

    private fun updateMapToLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f))
    }

    private fun setupNavigationDrawer() {
        val layoutManager = LinearLayoutManager(this)

        bookmarkRecyclerView.layoutManager = layoutManager
        bookmarkListAdapter = BookmarkListAdapter(null, this)
        bookmarkRecyclerView.adapter = bookmarkListAdapter
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer)
        toggle.syncState()
    }

    private fun createBookmarkObserver() {
        mapsViewModel.getBookmarkViews()?.observe(
                this, android.arch.lifecycle
                .Observer<List<MapsViewModel.SavedBookmarkView>> {
                    mMap.clear()
                    markers.clear()
                    it?.let {
                        displayAllBookmarks(it)
                        bookmarkListAdapter.setBookmarkData(it)
                    }
                })
    }

    private fun displayAllBookmarks(bookmarks: List<MapsViewModel.SavedBookmarkView>) {
        for (bookmark in bookmarks) {
            addPlaceMarker(bookmark)
        }
    }

    private fun addPlaceMarker(bookmark: MapsViewModel.SavedBookmarkView): Marker? {
        val marker = mMap.addMarker(MarkerOptions()
                .position(bookmark.location)
                .title(bookmark.name)
                .snippet(bookmark.phone)
                .icon(bookmark.categoryResourceId?.let {
                    BitmapDescriptorFactory.fromResource(it)
                })
                .alpha(0.8f))
        marker.tag = bookmark
        bookmark.id?.let { markers.put(it, marker) }
        return marker
    }

    private fun setupMapListeners() {
        mMap.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        mMap.setOnPoiClickListener {
            displayPoiDetails(it)
        }
        mMap.setOnInfoWindowClickListener {
            //            addBookmark(it)
            handleInfoWindowClick(it)
        }
        fab.setOnClickListener {
            searchAtCurrentLocation()
        }
        mMap.setOnMapLongClickListener {
            newBookmark(it)
        }
    }

    private fun setupViewModel() {
        mapsViewModel = ViewModelProviders.of(this).get(MapsViewModel::class.java)

        createBookmarkObserver()
    }

    private fun displayPoiDetails(poi: PointOfInterest) {
        showProgress()
        displayPoiGetPlaceStep(poi)
    }

    private fun displayPoiGetPlaceStep(poi: PointOfInterest) {
        Places.GeoDataApi.getPlaceById(googleApiClient, poi.placeId)
                .setResultCallback { placesBuffer ->
                    if (placesBuffer.status.isSuccess && placesBuffer.count > 0) {
                        val first = placesBuffer.get(0).freeze()
                        displayPoiGetPhotoMetaDataStep(first)
                    } else {
                        hideProgress()
                    }

                    placesBuffer.release()
                }
    }

    private fun displayPoiGetPhotoMetaDataStep(place: Place) {
        Places.GeoDataApi.getPlacePhotos(googleApiClient, place.id)
                .setResultCallback { placePhotoMetadataResult ->
                    if (placePhotoMetadataResult.status.isSuccess) {
                        val photoMetadataBuffer = placePhotoMetadataResult.photoMetadata
                        if (photoMetadataBuffer.count > 0) {
                            val photo = photoMetadataBuffer.get(0).freeze()

                            displayPoiGetPhotoStep(place, photo)
                        } else {
                            hideProgress()
                        }

                        photoMetadataBuffer.release()
                    } else {
                        hideProgress()
                    }
                }
    }

    private fun displayPoiGetPhotoStep(place: Place, photo: PlacePhotoMetadata) {
        photo.getScaledPhoto(googleApiClient,
                resources.getDimensionPixelSize(R.dimen.default_image_width),
                resources.getDimensionPixelSize(R.dimen.default_image_height))
                .setResultCallback { placePhotoResult ->
                    hideProgress()
                    if (placePhotoResult.status.isSuccess) {
                        val img = placePhotoResult.bitmap
                        displayPoiDisplayStep(place, img)
                    } else {
                        displayPoiDisplayStep(place, null)
                    }
                }
    }

    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {
        val marker = mMap.addMarker(MarkerOptions()
                .position(place.latLng)
                .title(place.name as String?)
                .snippet(place.phoneNumber as String?)
        )

        marker.tag = PlaceDetails(place, photo)

        marker?.showInfoWindow()
    }

    private fun setupGoogleClient() {
        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Places.GEO_DATA_API)
                .build()
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions()
        } else {
            mMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnCompleteListener {
                if (it.result != null) {
                    val lng = it.result.longitude
                    val lat = it.result.latitude

                    val location = LatLng(lng, lat)

                    val update = CameraUpdateFactory.newLatLngZoom(location, 10.0f)

                    mMap.moveCamera(update)
                }
            }
        }
    }

    private fun addBookmark(marker: Marker) {
        val placeDetails = marker.tag as PlaceDetails

        if (placeDetails.image != null) {
            launch(CommonPool) {
                mapsViewModel.addBookmarkFromPlace(placeDetails)
            }
        }

        marker.remove()
    }

    private fun handleInfoWindowClick(marker: Marker) {
        when (marker.tag) {
            is MapsActivity.PlaceDetails -> {
                val placeInfo = (marker.tag as PlaceDetails)
                if (placeInfo.place != null && placeInfo.image != null) {
                    launch(CommonPool) {
                        mapsViewModel.addBookmarkFromPlace(placeInfo.place,
                                placeInfo.image)
                    }
                }
                marker.remove();
            }
            is MapsViewModel.SavedBookmarkView -> {
                val bookmarkMarkerView = (marker.tag as
                        MapsViewModel.SavedBookmarkView)
                marker.hideInfoWindow()
                bookmarkMarkerView.id?.let {
                    startBookmarkDetails(it)
                }
            }
        }
    }

    private fun startBookmarkDetails(bookmarkId: Long) {
        val intent = Intent(this, TestActivity::class.java)
        intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
        startActivity(intent)
    }

    class PlaceDetails(val place: Place, val image: Bitmap?) {

    }

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val AUTOCOMPLETE_REQUEST_CODE = 2
        const val EXTRA_BOOKMARK_ID = "com.example.sgfs.placebook.EXTRA_BOOKMARK_ID"
    }
}
