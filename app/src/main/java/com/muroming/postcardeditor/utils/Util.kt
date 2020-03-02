package com.muroming.postcardeditor.utils

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat


fun Context.getCompatColor(@ColorRes color: Int) = ContextCompat.getColor(this, color)