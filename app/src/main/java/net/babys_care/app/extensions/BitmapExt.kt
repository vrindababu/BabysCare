package net.babys_care.app.extensions

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size

fun Bitmap.resized(longerSideLength: Int): Bitmap {
    val newSize: Size = if (width > height) {
        Size(longerSideLength, longerSideLength * height / width)
    } else {
        Size(longerSideLength * width / height, longerSideLength)
    }
    return Bitmap.createScaledBitmap(this, newSize.width, newSize.height, false)
}

fun Bitmap.rotate(): Bitmap {
    val matrix = Matrix().apply { this.setRotate(90F, width/2F, height/2F) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}