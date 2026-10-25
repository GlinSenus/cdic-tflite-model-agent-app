package com.frustration101.cdic


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.app.AlertDialog

import android.graphics.Bitmap
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.Gravity
import android.widget.TextView

class CameraActivity : AppCompatActivity() {

    private lateinit var viewFinder: androidx.camera.view.PreviewView
    private lateinit var imageView: ImageView
    private lateinit var captureButton: FloatingActionButton
    private lateinit var galleryButton: FloatingActionButton
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isShowingImage = false

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val imageUri: Uri? = data?.data
                if (imageUri != null) {
                    showImage(imageUri)
					classifyImage(imageUri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Toolbar setup
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        

        viewFinder = findViewById(R.id.viewFinder)
        imageView = findViewById(R.id.imageView)
        captureButton = findViewById(R.id.captureButton)
        galleryButton = findViewById(R.id.galleryButton)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request camera permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        captureButton.setOnClickListener {
            if (isShowingImage) {
                resetView()
            } else {
                takePhoto()
            }
        }

        galleryButton.setOnClickListener {
            openGallery()
        }
    }

    private fun takePhoto() {
		clearCapturedImages()
        val imageCapture = imageCapture ?: return
        val photoFile = File(externalMediaDirs.first(), "IMG_${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(applicationContext, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    showImage(savedUri)
					classifyImage(savedUri)
				
                }
            })
    }

    private fun showImage(uri: Uri) {
        imageView.setImageURI(uri)
        imageView.visibility = View.VISIBLE
        viewFinder.visibility = View.GONE
        captureButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        isShowingImage = true
    }

    private fun resetView() {
        imageView.visibility = View.GONE
        viewFinder.visibility = View.VISIBLE
        captureButton.setImageResource(android.R.drawable.ic_menu_camera)
        isShowingImage = false
        startCamera()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // Set the aspect ratio to 4:3 (portrait-friendly)
        val aspectRatio = androidx.camera.core.AspectRatio.RATIO_16_9
        val rotation = viewFinder.display.rotation

        val preview = Preview.Builder()
            .setTargetAspectRatio(aspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

		imageCapture = ImageCapture.Builder()
    		.setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    		.setTargetAspectRatio(aspectRatio)
    		.setTargetRotation(rotation)
   		 .build()
			
		viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
		    imageCapture?.targetRotation = viewFinder.display.rotation
		}

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
            )
        } catch (exc: Exception) {
            Toast.makeText(this, "Camera start failed: ${exc.message}", Toast.LENGTH_SHORT).show()
        }
    }, ContextCompat.getMainExecutor(this))
}

private fun classifyImage(imageData: Uri?) {
    try {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageData)

        // Initialize classifier
        val classifier = ImageClassifier(this@CameraActivity)

        // Classify image (returns top predictions)
        val result = classifier.classify(bitmap)

        // Close interpreter
        classifier.close()

        // Split result into lines (assuming: "Label (xx.xx%)" per line)
        val lines = result.split("\n")

        // Bold the first prediction
        val spannable = SpannableString("Prediction(s):\n\n$result")
        if (lines.isNotEmpty()) {
            val start = "Prediction(s):\n\n".length
            val end = start + lines[0].length
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Create a styled message view
        val messageView = TextView(this@CameraActivity).apply {
            text = spannable
            textSize = 16f
            setPadding(40, 20, 40, 20)
        }

        // Centered title
        val titleView = TextView(this@CameraActivity).apply {
            text = "Classification Result"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        // Build dialog
        AlertDialog.Builder(this@CameraActivity)
            .setCustomTitle(titleView)
            .setView(messageView) // use custom TextView for message
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()

    } catch (e: Exception) {
        e.printStackTrace()

        // Show toast only on failure
        Toast.makeText(
            this@CameraActivity,
            "Classification failed: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun clearCapturedImages() {
    val outputDir = externalMediaDirs.firstOrNull()
    outputDir?.listFiles()?.forEach { file ->
        if (file.name.startsWith("IMG_") && file.extension == "jpg") {
            file.delete()
        }
    }
}

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) startCamera()
            else Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Back arrow pressed
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
		clearCapturedImages()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}