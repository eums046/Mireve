package com.example.mireve

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import com.google.android.material.button.MaterialButton

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        loadTeamMemberPhotos()

        val btnBackToMain = findViewById<MaterialButton>(R.id.btnBackToMain)
        btnBackToMain.setOnClickListener {
            val intent = Intent(this, DiaryListActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadTeamMemberPhotos() {
        val member1ImageView = findViewById<ImageView>(R.id.ivMember1)
        val member2ImageView = findViewById<ImageView>(R.id.ivMember2)
        val member3ImageView = findViewById<ImageView>(R.id.ivMember3)
        val member4ImageView = findViewById<ImageView>(R.id.ivMember4)

        loadMemberPhoto(member1ImageView, R.drawable.member_1)
        loadMemberPhoto(member2ImageView, R.drawable.member_2)
        loadMemberPhoto(member3ImageView, R.drawable.member_3)
        loadMemberPhoto(member4ImageView, R.drawable.member_4)
    }

    private fun loadMemberPhoto(imageView: ImageView, photoResourceId: Int) {
        try {
            val bitmap = loadScaledBitmap(photoResourceId)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
            } else {
                imageView.setImageResource(photoResourceId)
            }
        } catch (e: Exception) {
            Log.e("AboutActivity", "Error loading photo: $photoResourceId", e)
            imageView.setImageResource(photoResourceId)
        }
    }

    private fun loadScaledBitmap(photoResourceId: Int): Bitmap? {
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeResource(resources, photoResourceId, options)

            val maxDimension = 800
            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
            options.inJustDecodeBounds = false

            val originalBitmap = BitmapFactory.decodeResource(resources, photoResourceId, options)
                ?: return null

            val bitmapSize = originalBitmap.byteCount
            val maxSize = 50 * 1024 * 1024

            if (bitmapSize > maxSize) {
                Log.w("AboutActivity", "Bitmap still too large: ${bitmapSize / (1024 * 1024)}MB")
                val scaleFactor = kotlin.math.sqrt(maxSize.toDouble() / bitmapSize).toFloat()
                val newWidth = (originalBitmap.width * scaleFactor).toInt()
                val newHeight = (originalBitmap.height * scaleFactor).toInt()

                val scaledBitmap = originalBitmap.scale(newWidth, newHeight, true)
                originalBitmap.recycle()
                return scaledBitmap
            }

            return originalBitmap

        } catch (e: Exception) {
            Log.e("AboutActivity", "Error in loadScaledBitmap", e)
            return null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}