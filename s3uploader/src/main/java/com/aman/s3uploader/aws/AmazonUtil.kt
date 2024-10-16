package com.aman.s3uploader.aws

import android.content.Context
import android.net.Uri
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

object AmazonUtil {

    private var sS3Client: AmazonS3Client? = null
    private var sCredProvider: CognitoCachingCredentialsProvider? = null
    private var sTransferUtility: TransferUtility? = null

    /**
     * Gets an instance of CognitoCachingCredentialsProvider which is
     * constructed using the given Context.
     *
     * @param context An Context instance.
     * @param region The AWS region.
     * @param cognitoPoolId The Cognito pool ID.
     * @return A default credential provider.
     */
    fun getCredProvider(context: Context, region: Regions, cognitoPoolId: String): CognitoCachingCredentialsProvider {
        if (sCredProvider == null) {
            // Initialize the Amazon Cognito credentials provider
            sCredProvider = CognitoCachingCredentialsProvider(
                context,
                cognitoPoolId,
                region
            )
        }
        return sCredProvider!!
    }

    /**
     * Gets an instance of a S3 client which is constructed using the given
     * Context.
     *
     * @param context An Context instance.
     * @param region The AWS region.
     * @param cognitoPoolId The Cognito pool ID.
     * @return A default S3 client.
     */
    fun getS3Client(context: Context, region: Regions, cognitoPoolId: String): AmazonS3Client {
        if (sS3Client == null) {
            sS3Client = AmazonS3Client(getCredProvider(context, region, cognitoPoolId))
            sS3Client!!.setRegion(Region.getRegion(region))
        }
        return sS3Client!!
    }

    /**
     * Gets an instance of the TransferUtility which is constructed using the
     * given Context
     *
     * @param context An Context instance.
     * @param region The AWS region.
     * @param cognitoPoolId The Cognito pool ID.
     * @return a TransferUtility instance
     */
    fun getTransferUtility(context: Context, region: Regions, cognitoPoolId: String): TransferUtility {
        if (sTransferUtility == null) {
            sTransferUtility = TransferUtility.builder()
                .s3Client(getS3Client(context.applicationContext, region, cognitoPoolId))
                .context(context.applicationContext)
                .build()
        }
        return sTransferUtility!!
    }

    /**
     * Converts number of bytes into proper scale.
     *
     * @param bytes number of bytes to be converted.
     * @return A string that represents the bytes in a proper scale.
     */
    fun getBytesString(bytes: Long): String {
        val quantifiers = arrayOf("KB", "MB", "GB", "TB")
        var speedNum = bytes.toDouble()
        for (quantifier in quantifiers) {
            speedNum /= 1024
            if (speedNum < 512) {
                return String.format("%.2f %s", speedNum, quantifier)
            }
        }
        return ""
    }

    /**
     * Copies the data from the passed in Uri, to a new file for use with the
     * Transfer Service
     *
     * @param context
     * @param uri
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun copyContentUriToFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val copiedData = File(context.getDir("SampleImagesDir", Context.MODE_PRIVATE), UUID.randomUUID().toString())
        copiedData.createNewFile()

        FileOutputStream(copiedData).use { outputStream ->
            val buffer = ByteArray(2048)
            var bytesRead: Int
            while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        }
        inputStream?.close()
        return copiedData
    }
}
