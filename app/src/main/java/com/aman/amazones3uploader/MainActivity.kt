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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

    private val SELECT_PICTURE = 1
    private var count: Int = 0

    private val progressDialog: ProgressDialog by lazy {
        ProgressDialog(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initViews()
        listeners()

    }

    private fun initViews() {
        s3uploaderObj = S3Uploader(this)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    chooseImage()
                } else {
                    chooseImage()
                    Log.v(TAG, "Permission is revoked")
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1
                    )
                }
            } else {
                Log.v(TAG, "Permission is granted")
                chooseImage()
            }
    }

    private fun chooseImage() {
        val intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            chooseImage()
            Log.e(TAG, "Permission: " + permissions[0] + "was " + grantResults[0])
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
        val path: String = getFilePathFromURI(this,imageUri)!!
        if (path != null) {
            showLoading()
            Log.d(TAG, "uploadImageTos3: Path: $path")
            s3uploaderObj.initUpload(AWSKeys.MY_REGION, AWSKeys.BUCKET_NAME, path, AWSKeys.COGNITO_POOL_ID)
            s3uploaderObj.setOnS3UploadDone(object : S3Uploader.S3UploadInterface {

                override fun onUploadSuccess(response: String) {
                    if (response.equals("Success", ignoreCase = true)) {
                        hideLoading()
                        urlFromS3 = S3Utils.generateS3ShareUrl(applicationContext,AWSKeys.MY_REGION, AWSKeys.BUCKET_NAME,path, AWSKeys.COGNITO_POOL_ID)
                        if (!TextUtils.isEmpty(urlFromS3)) {
                            Log.d(TAG, "onUploadSuccess: Image Url: $urlFromS3")
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PICTURE) {
            if (resultCode == RESULT_OK && data != null) {
                onPictureSelect(data)
            }
        }
    }

}