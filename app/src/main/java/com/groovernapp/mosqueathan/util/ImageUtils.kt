package com.groovernapp.mosqueathan.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    /**
     * Decodes, downscales to target dimensions (defaults to 1920x1080),
     * and compresses the image at [file] in-place with 90% quality to preserve visual clarity.
     */
    fun downscaleImageInPlace(file: File, reqWidth: Int = 1920, reqHeight: Int = 1080) {
        if (!file.exists()) return
        try {
            // 1. Inspect original dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            val height = options.outHeight
            val width = options.outWidth
            if (height <= 0 || width <= 0) return

            // 2. Downscale only if dimensions exceed target bounds
            if (height > reqHeight || width > reqWidth) {
                var inSampleSize = 1
                val halfHeight = height / 2
                val halfWidth = width / 2

                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }

                options.inSampleSize = inSampleSize
                options.inJustDecodeBounds = false

                val decodedBitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                if (decodedBitmap != null) {
                    FileOutputStream(file).use { out ->
                        // Using 90% compression quality to satisfy the requirement: "Keep the quality"
                        decodedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    decodedBitmap.recycle()
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}