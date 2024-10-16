package com.aman.amazones3uploader

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.aman.amazones3uploader.databinding.ActivityMainBinding
import com.aman.amazones3uploader.utils.AWSKeys
import com.aman.amazones3uploader.utils.getFilePathFromURI
import com.aman.s3uploader.aws.S3Uploader
import com.aman.s3uploader.aws.S3Utils
import java.io.FileNotFoundException
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var s3uploaderObj: S3Uploader

    private var urlFromS3: String? = null
    private lateinit var imageUri: Uri

    private var count: Int = 0

    private val progressDialog: ProgressDialog by lazy {
        ProgressDialog(this)
    }

    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initViews()
        initLaunchers()
        listeners()

    }

    private fun initViews() {
        s3uploaderObj = S3Uploader(this)
    }

    private fun initLaunchers() {
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                chooseImage()
                Log.v(TAG, "Permission is granted")
            } else {
                Log.e(TAG, "Please click again and select allow to choose profile picture")
            }
        }

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                onPictureSelect(result.data!!)
            }
        }
    }

    private fun listeners() {
        binding.apply {
            btSelect.setOnClickListener {
                isStoragePermissionGranted()
            }

            btUpload.setOnClickListener {
                if (count > 0) {
                    if (imageUri != null) {
                        Log.d(TAG, "onClick: Image Uri: $imageUri")
                        uploadImageTos3(imageUri)
                    } else {
                        Toast.makeText(this@MainActivity, "image uri is empty", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Choose image first", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun isStoragePermissionGranted() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {

                if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    chooseImage()
                } else {
                    Log.v(TAG, "Permission is revoked")
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {

                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    chooseImage()
                } else {
                    Log.v(TAG, "Permission is revoked")
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            else -> {

                Log.v(TAG, "Permission is granted")
                chooseImage()
            }
        }
    }

    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            chooseImage()
            Log.e(TAG, "Permission: " + permissions[0] + " was " + grantResults[0])
        } else {
            Log.e(TAG, "Please click again and select allow to choose profile picture")
        }
    }

    private fun onPictureSelect(data: Intent) {
        imageUri = data.data!!
        var imageStream: InputStream? = null
        Log.d(TAG, "OnPictureSelect: $imageUri")
        try {
            imageStream = contentResolver.openInputStream(imageUri)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        if (imageStream != null) {
            count++
            binding.image.setImageBitmap(BitmapFactory.decodeStream(imageStream))
        }
    }

    private fun uploadImageTos3(imageUri: Uri) {
        getFilePathFromURI(this,imageUri)?.let {
//            val path: String = getFilePathFromURI(this,imageUri)!!
            if (it != null) {
                showLoading()
                Log.d(TAG, "uploadImageTos3: Path: $it")
                s3uploaderObj.initUpload(AWSKeys.MY_REGION, AWSKeys.BUCKET_NAME, it, AWSKeys.COGNITO_POOL_ID)
                s3uploaderObj.setOnS3UploadDone(object : S3Uploader.S3UploadInterface {

                    override fun onUploadSuccess(response: String) {
                        if (response.equals("Success", ignoreCase = true)) {
                            hideLoading()
                            urlFromS3 = S3Utils.generateS3ShortUrl(AWSKeys.MY_REGION, AWSKeys.BUCKET_NAME,it)
                            if (!TextUtils.isEmpty(urlFromS3)) {
                                Log.d(TAG, "onUploadSuccess: Image Url: $urlFromS3")
                                binding.edtS3Url.setText(urlFromS3.toString())
                                Toast.makeText(
                                    this@MainActivity,
                                    "Uploaded Successfully!!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    override fun onUploadError(response: String) {
                        hideLoading()
                        Log.e(TAG, "Error Uploading $response")
                    }
                })
            } else {
                Toast.makeText(this, "Null Path", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading() {
        if (progressDialog != null && !progressDialog.isShowing) {
            progressDialog.setMessage("Uploading Image !!")
            progressDialog.setCancelable(false)
            progressDialog.show()
        }
    }

    private fun hideLoading() {
        if (progressDialog != null && progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }
}