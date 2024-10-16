package com.aman.s3uploader.aws

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.ObjectMetadata
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class S3Uploader(private val context: Context) {

    private lateinit var transferUtility: TransferUtility
    var s3UploadInterface: S3UploadInterface? = null

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
                throw FileNotFoundException("File does not exist or is not a valid file")
            }

            transferUtility = AmazonUtil.getTransferUtility(context, region, cognitoPoolId)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val uniqueFileName = "${file.name}_${UUID.randomUUID()}_${timestamp}".replace(" ", "_").replace("-", "_")
            S3Utils.uniqueFileName = uniqueFileName

            val myObjectMetadata = ObjectMetadata().apply {
                contentType = "image/png"
            }

            val mediaUrl = "Images/$uniqueFileName"
            Log.d(TAG, "initUpload: File: $file mediaUrl: $mediaUrl")

            val observer = transferUtility.upload(bucketName, mediaUrl, file)
            observer.setTransferListener(UploadListener())

        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "initUpload: Invalid argument - ${e.message}")
            // Handle specific case when file path is invalid
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "initUpload: File not found - ${e.message}")
            // Handle case when the file doesn't exist
        } catch (e: AmazonServiceException) {
            Log.e(TAG, "initUpload: Amazon service error - ${e.message}")
            // Handle errors related to AWS S3 service, such as permission issues
        } catch (e: AmazonClientException) {
            Log.e(TAG, "initUpload: Amazon client error - ${e.message}")
            // Handle errors that occur when the client can't connect to AWS S3
        } catch (e: Exception) {
            Log.e(TAG, "initUpload: Unexpected error occurred - ${e.message}")
        }
    }



    private inner class UploadListener : TransferListener {

        override fun onError(id: Int, e: Exception) {
            Log.e(TAG, "Error during upload: $id", e)
            s3UploadInterface?.onUploadError(e.toString())
            s3UploadInterface?.onUploadError("Error")
        }

        override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            val percentage = (bytesCurrent.toDouble() / bytesTotal * 100).toInt()
            Log.d(TAG, "onProgressChanged: $id, total: $bytesTotal, current: $bytesCurrent, percentage: $percentage")

        }

        override fun onStateChanged(id: Int, newState: TransferState) {
            Log.d(TAG, "onStateChanged: $id, $newState")
            if (newState == TransferState.COMPLETED) {
                s3UploadInterface?.onUploadSuccess("Success")
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
