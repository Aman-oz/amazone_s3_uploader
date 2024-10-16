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

    var uniqueFileName: String = ""

    /**
     * Method to generate a presigned URL for the image
     * @param applicationContext context
     * @param region AWS region
     * @param path image path
     * @param cognitoPoolId AWS Cognito pool ID
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

    /**
     * Method to generate a short URL for the image
     * @param region AWS region
     * @param bucketName AWS S3 bucket name
     * @param path image path
     * @return short URL as a string
     * */

    fun generateS3ShortUrl(region: Regions, bucketName: String, path: String): String {
        val file = File(path)
        val fileName = file.name
        val mediaUrl = "https://$bucketName.s3.${region.getName()}.amazonaws.com/Images/$uniqueFileName"

        Log.d(TAG, "Generated S3 URL: $mediaUrl")
        return mediaUrl
    }
}