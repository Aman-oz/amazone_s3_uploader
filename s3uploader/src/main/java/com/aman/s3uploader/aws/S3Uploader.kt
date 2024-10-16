package com.aman.s3uploader.aws

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.ObjectMetadata
import java.io.File
import java.io.FileNotFoundException
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class S3Uploader(private val context: Context) {

    private lateinit var transferUtility: TransferUtility
    var s3UploadInterface: S3UploadInterface? = null
    private val internetManager: InternetManager by lazy {
        InternetManager(context)
    }

    /**
     * Initiates the upload of a file to S3.
     * @param filePath Path of the file to be uploaded.
     */
    fun initUpload1(region: Regions, bucketName: String,filePath: String, cognitoPoolId: String) {
        transferUtility = AmazonUtil.getTransferUtility(context, region, cognitoPoolId)
        val file = File(filePath)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val uniqueFileName = "${file.name}_${UUID.randomUUID()}_${timestamp}".replace(" ", "_").replace("-", "_")
        S3Utils.uniqueFileName = uniqueFileName

        val myObjectMetadata = ObjectMetadata()
        myObjectMetadata.contentType = "image/png"
        val mediaUrl = "Images/${uniqueFileName}"
        Log.d(TAG, "initUpload: File: $file mediaUrl: $mediaUrl")
        val observer = transferUtility.upload(bucketName, mediaUrl, file)
        observer.setTransferListener(UploadListener())
    }

    /**
     * Initiates the upload of a file to S3.
     * @param region AWS region.
     * @param bucketName Name of the S3 bucket.
     * @param filePath Path of the file to be uploaded.
     * @param cognitoPoolId ID of the Cognito user pool.
     * @throws IllegalArgumentException If the file path is empty.
     * @throws FileNotFoundException If the file doesn't exist or is not a valid file.
     * @throws AmazonServiceException If there's an error with the AWS S3 service.
     * @throws AmazonClientException If there's an error with the AWS client.
     * @throws Exception If an unexpected error occurs.
     */
    fun initUpload(region: Regions, bucketName: String, filePath: String, cognitoPoolId: String) {
        try {
            if (filePath.isBlank()) {
                throw IllegalArgumentException("File path is empty")
            }

            val file = File(filePath)
            if (!file.exists() || !file.isFile) {
                throw FileNotFoundException("File does not exist or is not a valid file: -> $filePath\n---------Solution: Select a valid file----------\ne.g: -> /storage/emulated/0/Pictures/image.jpg\nKey Points:\n1. File should exist.\n2. File should be a valid image.\n3. Should be a storage path.\n4. Should not be a file uri or content uri.")
            }

            transferUtility = AmazonUtil.getTransferUtility(context, region, cognitoPoolId)

            if (transferUtility == null) {
                Log.e(TAG, "initUpload: TransferUtility initialization failed")
                s3UploadInterface?.onUploadError("Initialization failed")
                return
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val uniqueFileName = "${file.name}_${UUID.randomUUID()}_${timestamp}".replace(" ", "_").replace("-", "_").replace(".","_")
            S3Utils.uniqueFileName = uniqueFileName

            val contentType = URLConnection.guessContentTypeFromName(filePath) ?: "image/*"
            val myObjectMetadata = ObjectMetadata().apply {
                this.contentType = contentType
            }

            val mediaUrl = "Images/$uniqueFileName"
            Log.d(TAG, "initUpload: File: $file mediaUrl: $mediaUrl")

            val observer = transferUtility.upload(bucketName, mediaUrl, file)
            observer.setTransferListener(UploadListener())

        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "initUpload: Invalid argument - ${e.message}")
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "initUpload: File not found - ${e.message}")
        } catch (e: AmazonServiceException) {
            Log.e(TAG, "initUpload: Amazon service error - ${e.message}")
        } catch (e: AmazonClientException) {
            Log.e(TAG, "initUpload: Amazon client error - ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "initUpload: Unexpected error occurred - ${e.message}")
        }
    }

    /**Upload Listener
     * Listens for upload events and handles them accordingly.
     * */

    private inner class UploadListener : TransferListener {

        override fun onError(id: Int, e: Exception) {
            Log.e(TAG, "Error during upload: $id", e)
            val errorMessage = "Upload failed: ${e.localizedMessage ?: "Unknown error"}"
            s3UploadInterface?.onUploadError(errorMessage)
        }

        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            val percentage = (bytesCurrent.toDouble() / bytesTotal * 100).toInt()
            Log.d(TAG, "onProgressChanged: $id, total: $bytesTotal, current: $bytesCurrent, percentage: $percentage")

        }

        override fun onStateChanged(id: Int, newState: TransferState) {
            Log.d(TAG, "onStateChanged: $id, $newState")

            when (newState) {
                TransferState.COMPLETED -> s3UploadInterface?.onUploadSuccess("Success")
                TransferState.FAILED -> s3UploadInterface?.onUploadError("Upload failed")
                TransferState.CANCELED -> s3UploadInterface?.onUploadError("Upload canceled")
                TransferState.IN_PROGRESS -> Log.d(TAG, "Upload is in progress")
                else -> Log.d(TAG, "Unhandled state: $newState")
            }
        }
    }

    /**
     * Sets the callback interface for upload events.
     * @param s3UploadInterface Interface instance to handle upload events.
     */
    fun setOnS3UploadDone(s3UploadInterface: S3UploadInterface) {
        this.s3UploadInterface = s3UploadInterface
    }

    interface S3UploadInterface {
        fun onUploadSuccess(response: String)
        fun onUploadError(response: String)
    }

    companion object {
        private const val TAG = "S3UploaderTAG"
    }
}
