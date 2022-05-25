package com.example.drawme

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.drawme.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(){

    private lateinit var binding: ActivityMainBinding
    private var mImageButtonCurrentPaint: ImageButton?=null
    private lateinit var colorDialog: Dialog

    var customProgressDialog: Dialog? = null


    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                binding.ivBackground.setImageURI(it.data?.data)
            }
        }

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            permission.entries.forEach {
                var permissionName=it.key
                var isGranted=it.value

                if (isGranted) {
                    var pi=Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pi)
                } else {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            applicationContext,
                            "not permission read storage",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.drawingView

        // Button Click event
        binding.drawingView.setSizeForBrush(5.toFloat())

        binding.ibBrash.setOnClickListener {
            showBrashSizeChooserDialog()
        }

        binding.ibColor.setOnClickListener {
            showColorChooserDialog()
        }
        binding.ibClear.setOnClickListener {
            binding.drawingView.clear()
        }
        binding.ibUndo.setOnClickListener {
            binding.drawingView.undo()
        }

        binding.ibRedo.setOnClickListener {
            binding.drawingView.redo()
        }

        binding.ibGallery.setOnClickListener {
            requestStoragePermission()
        }
        binding.ibSave.setOnClickListener {
            Log.d("save","Save or not")
            showProgressDialog()
            if (readStorageAllowed()){
                lifecycleScope.launch {
                    saveBitmapFile(getBitmapFromView(binding.flDrawingViewContainer))
                }
            }
        }
    }

    private fun getBitmapFromView(view: View):Bitmap{
        val returnBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnBitmap)
        var bgDrawable = view.background

        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap):String{
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {

                try {
                    val bytes = ByteArrayOutputStream()

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "Drawme" + System.currentTimeMillis() / 1000 + ".png"
                    )
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully :$result",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun shareImage(result:String){

        MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){ _, uri ->

            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type ="image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))

        }
    }

    private fun readStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            alertDialog("Draw me", "Need to access your storage")
            Log.e("Tag","request permission")
        } else {
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun alertDialog(title: String, message: String) {
        val builder: AlertDialog.Builder=AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun showColorChooserDialog() {
        colorDialog=Dialog(this)
        colorDialog.setContentView(R.layout.dialog_choose_color)
        colorDialog.setTitle("Brash Size: ")
        colorDialog.window?.attributes?.gravity=Gravity.BOTTOM or Gravity.BOTTOM
        colorDialog.show()

    }

    fun paintClicked(view: View) {
        if (view !== mImageButtonCurrentPaint) {
            val imageButton=view as ImageButton
            val colorTag=imageButton.tag.toString()
            binding.drawingView.setColor(colorTag)
            imageButton.isSelected=true

            if (imageButton.isSelected) {
                colorDialog.dismiss()
            }
            mImageButtonCurrentPaint=view

        }
    }


    @SuppressLint("RtlHardcoded")
    private fun showBrashSizeChooserDialog() {

        val brashDialog=Dialog(this)
        brashDialog.setContentView(R.layout.dialog_brash_size)
        brashDialog.setTitle("Brash Size: ")
        brashDialog.window?.attributes?.gravity=Gravity.BOTTOM or Gravity.BOTTOM


        var smallBtn=brashDialog.findViewById<ImageButton>(R.id.ibSmallBrash)
        smallBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(10.toFloat())
            binding.drawingView
            brashDialog.dismiss()
        }

        var small1Btn=brashDialog.findViewById<ImageButton>(R.id.ibSmall1Brash)
        small1Btn.setOnClickListener {
            binding.drawingView.setSizeForBrush(15.toFloat())
            brashDialog.dismiss()
        }

        var mediumBtn=brashDialog.findViewById<ImageButton>(R.id.ibMediumBrash)
        mediumBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(20.toFloat())
            brashDialog.dismiss()
        }

        var medium1Btn=brashDialog.findViewById<ImageButton>(R.id.ibMedium1Brash)
        medium1Btn.setOnClickListener {
            binding.drawingView.setSizeForBrush(25.toFloat())
            brashDialog.dismiss()
        }

        var largeBtn=brashDialog.findViewById<ImageButton>(R.id.ibLargeBrash)
        largeBtn.setOnClickListener {
            binding.drawingView.setSizeForBrush(30.toFloat())
            brashDialog.dismiss()
        }

        var large1Btn=brashDialog.findViewById<ImageButton>(R.id.ibLarge1Brash)
        large1Btn.setOnClickListener {
            binding.drawingView.setSizeForBrush(35.toFloat())
            brashDialog.dismiss()
        }

        brashDialog.show()
    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }
    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }


}