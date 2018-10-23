package com.example.sgfs.placebook.ui

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.sgfs.placebook.R
import com.example.sgfs.placebook.util.ImageUtils
import com.example.sgfs.placebook.viewmodel.BookmarkDetailsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_details.*
import java.io.File
import java.io.IOException
import java.net.URLEncoder

class TestActivity : AppCompatActivity(), PhotoOptionDialogFragment.PhotoOptionsDialogListener {

    private lateinit var bookmarkDetailsViewModel: BookmarkDetailsViewModel
    private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null
    private var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark_details)

        setupViewModel()
        getIntentData()
        setupFab()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CAPTURE_IMAGE -> {
                    photoFile ?: return

                    val fileUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, photoFile!!)
                    revokeUriPermission(fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                    val imageWithPath = getImageWithPath(photoFile!!.absolutePath)
                    imageWithPath?.let { updateImage(imageWithPath) }
                }
                REQUEST_GALLERY_IMAGE ->
                    if (data != null && data.data != null) {
                        val imageUri = data.data
                        val image = getImageWithAuthority(imageUri)
                        image?.let { updateImage(it) }
                    }
            }
        }
    }

    override fun onCaptureClick() {
        photoFile = null

        try {
            photoFile = ImageUtils.createUniqueImageFile(this)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return
        }

        val imageCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val fileUri = FileProvider.getUriForFile(this, "com.example.sgfs.placebook.fileprovider", photoFile!!)
        imageCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)

        val intentActivities = packageManager.queryIntentActivities(imageCaptureIntent, PackageManager.MATCH_DEFAULT_ONLY)
        intentActivities.map { it.activityInfo.packageName }
                .forEach {
                    grantUriPermission(it, fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

        startActivityForResult(imageCaptureIntent, REQUEST_CAPTURE_IMAGE)
    }

    override fun onPickClick() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_bookmark_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                saveChanges()
                return true
            }
            R.id.action_delete -> {
                deleteBookmark()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setupFab() {
        fab.setOnClickListener { sharePlace() }
    }

    private fun sharePlace() {
        val bookmarkView = bookmarkDetailsView ?: return
        var mapUrl = ""

        if (bookmarkView.placeId == null) {
            val location = URLEncoder.encode("${bookmarkView.latitude}," + "${bookmarkView.longitude}", "utf-8")
            mapUrl = "https://www.google.com/maps/dir/?api=1" + "&destination=$location"
        } else {
            val name = URLEncoder.encode(bookmarkView.name, "utf-8")
            mapUrl = "https://www.google.com/maps/dir/?api=1" +
                    "&destination=$name&destination_place_id=" +
                    "${bookmarkView.placeId}"
        }

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out ${bookmarkView.name} at:\n$mapUrl")
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing ${bookmarkView.name}")
        sendIntent.type = "text/plain"
        startActivity(sendIntent)
    }

    private fun populateCategoryList() {
        val bookmarkView = bookmarkDetailsView ?: return

        val resourceId = bookmarkDetailsViewModel.getCategoryResourceIdFromName(bookmarkView.category)

        resourceId?.let { imageViewCategory.setImageResource(it) }

        val categories = bookmarkDetailsViewModel.getCategories()

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerCategory.adapter = adapter

        val placeCategory = bookmarkView.category
        spinnerCategory.setSelection(adapter.getPosition(placeCategory))

        spinnerCategory.post {
            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val category = parent.getItemAtPosition(position) as String
                    val resourceIdNew = bookmarkDetailsViewModel.getCategoryResourceIdFromName(category)
                    resourceIdNew?.let {
                        imageViewCategory.setImageResource(it)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // NOTE: This method is required but not used.
                }
            }
        }
    }

    private fun getImageWithAuthority(uri: Uri): Bitmap? {
        return ImageUtils.decodeUriStreamToSize(uri,
                resources.getDimensionPixelSize(R.dimen.default_image_width),
                resources.getDimensionPixelSize(R.dimen.default_image_height),
                this)
    }

    private fun getImageWithPath(filePath: String): Bitmap? {
        return ImageUtils.decodeFileToSize(filePath,
                resources.getDimensionPixelSize(R.dimen.default_image_width),
                resources.getDimensionPixelSize(R.dimen.default_image_height)
        )
    }

    private fun updateImage(image: Bitmap) {
        val bookmarkView = bookmarkDetailsView ?: return
        imageViewPlace.setImageBitmap(image)
        bookmarkView.setImage(this, image)
    }

    private fun changeImage() {
        val photoOptionDialogFragment = PhotoOptionDialogFragment.newInstance(this)
        photoOptionDialogFragment?.show(supportFragmentManager, "photoOptionDialog")
    }

    private fun saveChanges() {
        val name = editTextName.text.toString()
        if (name.isEmpty()) {
            return
        }
        bookmarkDetailsView?.let { bookmarkView ->
            bookmarkView.name = editTextName.text.toString()
            bookmarkView.notes = editTextNotes.text.toString()
            bookmarkView.address = editTextAddress.text.toString()
            bookmarkView.phone = editTextPhone.text.toString()
            bookmarkView.category = spinnerCategory.selectedItem.toString()
            bookmarkDetailsViewModel.updateBookmark(bookmarkView)
        }
        finish()
    }

    private fun deleteBookmark() {
        val bookmarkView = bookmarkDetailsView ?: return
        AlertDialog.Builder(this)
                .setMessage("Delete?")
                .setPositiveButton("Ok") { _, _ ->
                    bookmarkDetailsViewModel.deleteBookmark(bookmarkView)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .create().show()
    }

    private fun getIntentData() {
        val bookmarkId = intent.getLongExtra(MapsActivity.Companion.EXTRA_BOOKMARK_ID, 0)

        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(
                this, Observer<BookmarkDetailsViewModel.BookmarkDetailsView> {
            it.let {
                bookmarkDetailsView = it
                populateFields()
                populateImageView()
                populateCategoryList()
            }
        })
    }

    private fun setupViewModel() {
        bookmarkDetailsViewModel = ViewModelProviders.of(this).get(BookmarkDetailsViewModel::class.java)
    }

    private fun populateFields() {
        bookmarkDetailsView?.let { bookmarkView ->
            editTextName.setText(bookmarkView.name)
            editTextPhone.setText(bookmarkView.phone)
            editTextNotes.setText(bookmarkView.notes)
            editTextAddress.setText(bookmarkView.address)
        }
    }

    private fun populateImageView() {
        bookmarkDetailsView?.let { bookmarkView ->
            val placeImage = bookmarkView.getImage(this)
            placeImage?.let { it ->
                imageViewPlace.setImageBitmap(placeImage)

                imageViewPlace.setOnClickListener {
                    changeImage()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CAPTURE_IMAGE = 1
        private const val REQUEST_GALLERY_IMAGE = 2
        private const val FILE_PROVIDER_AUTHORITY = "com.example.sgfs.placebook.fileprovider"
    }
}
