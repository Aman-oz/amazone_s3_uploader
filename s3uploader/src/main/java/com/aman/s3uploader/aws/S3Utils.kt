package com.aman.s3uploader.aws

import android.content.Context
import android.util.Log
import com.amazonaws.HttpMethod
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import java.io.File
import java.net.URL

object S3Utils {
    private val TAG = S3Utils::class.java.simpleName

    /**
     * Method to generate a presigned URL for the image
     * @param applicationContext context
     * @param path image path
     * @return presigned URL
     */
    fun generateS3ShareUrl(applicationContext: Context, region: Regions, bucketName: String, path: String, cognitoPoolId: String): String {
        val file = File(path)
        Log.d(TAG, "generateS3ShareUrl: Path: $path")
        val s3client = AmazonUtil.getS3Client(applicationContext,region, cognitoPoolId)

        val mediaUrl = "Images/" + file.name

        val generatePresignedUrlRequest = GeneratePresignedUrlRequest(bucketName, mediaUrl)
        generatePresignedUrlRequest.method = HttpMethod.GET

        val url: URL = s3client.generatePresignedUrl(generatePresignedUrlRequest)
        Log.e("s", url.toString())
        return url.toString()
    }
}