package com.example.sgfs.placebook.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.example.sgfs.placebook.R

class PhotoOptionDialogFragment : DialogFragment() {
    private var listener: PhotoOptionsDialogListener? = null

    private val CAMERA = "Camera"
    private val GALLERY = "Gallery"
    private val PHOTO_OPTION = "Photo Option"
    private val CANCEL = "Cancel"

    companion object {
        fun canPick(cnx: Context): Boolean {
            val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            return (pickIntent.resolveActivity(cnx.packageManager) != null)
        }

        fun canCapture(cnx: Context): Boolean {
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            return (captureIntent.resolveActivity(cnx.packageManager) != null)
        }

        @JvmStatic
        fun newInstance(cnx: Context): PhotoOptionDialogFragment? {
            if (canCapture(cnx) || canPick(cnx))
                return PhotoOptionDialogFragment()

            return null
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        listener = activity as PhotoOptionsDialogListener

        var captureSelectIdx = -1
        var pickSelectIdx = -1

        val options = ArrayList<String>()

        if (canCapture(this.context!!)) {
            options.add(CAMERA)
            captureSelectIdx = 0
        }

        if (canPick(this.context!!)) {
            options.add(GALLERY)
            pickSelectIdx = if (captureSelectIdx == 0) 1 else 0
        }

        return AlertDialog.Builder(activity!!)
                .setTitle(PHOTO_OPTION)
                .setItems(options.toTypedArray<CharSequence>()) { _, which ->
                    if (which == captureSelectIdx) {
                        listener!!.onCaptureClick()
                    } else if (which == pickSelectIdx) {
                        listener!!.onPickClick()
                    }
                }
                .setNegativeButton(CANCEL, null)
                .create()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_photo_option_dialog, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PhotoOptionsDialogListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface PhotoOptionsDialogListener {
        fun onCaptureClick()
        fun onPickClick()
    }

}
