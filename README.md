# Amazon S3 Uploader

## Project Description
Amazon S3 Uploader is an Android library for uploading images to Amazon S3. 
This library simplifies the process of integrating Amazon S3 into your Android application, 
allowing for quick and efficient image uploads.

## How to Use the Project

### Step 1: Add the Library to Your Project

Add the following to your project's `build.gradle` file:
```gradle
maven { url 'https://jitpack.io' }
implementation 'com.github.Aman-oz:amazone_s3_uploader:$latest'
```

### Step 2: Initialize and Use the S3Uploader in Your Activity or Fragment
**In your activity or fragment, initialize the S3Uploader object and use it to upload images. Below is a sample implementation:**
```
private lateinit var s3uploaderObj: S3Uploader
private var urlFromS3: String? = null
private val progressDialog: ProgressDialog by lazy {
    ProgressDialog(this)
}

s3uploaderObj = S3Uploader(this)

private fun uploadImageTos3(imageUri: Uri) {
    val path: String = getFilePathFromURI(this, imageUri)!!
    if (path != null) {
        showLoading()
        Log.d(TAG, "uploadImageTos3: Path: $path")
        s3uploaderObj.initUpload(AWSKeys.MY_REGION, AWSKeys.BUCKET_NAME, path, AWSKeys.COGNITO_POOL_ID)
        s3uploaderObj.setOnS3UploadDone(object : S3Uploader.S3UploadInterface {
            override fun onUploadSuccess(response: String) {
                if (response.equals("Success", ignoreCase = true)) {
                    hideLoading()
                    urlFromS3 = S3Utils.generateS3ShareUrl(applicationContext, AWSKeys.MY_REGION, AWSKeys.BUCKET_NAME, path, AWSKeys.COGNITO_POOL_ID)
                    if (!TextUtils.isEmpty(urlFromS3)) {
                        Log.d(TAG, "onUploadSuccess: Image Url: $urlFromS3")
                        Toast.makeText(this@MainActivity, "Uploaded Successfully!!", Toast.LENGTH_SHORT).show()
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

```

## Credits
* Library Author: Aman-oz
* Amazon S3: Amazon Web Services (AWS)


