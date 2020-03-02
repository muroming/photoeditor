package com.muroming.postcardeditor.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.ui.fragments.PhotoEditorFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction()
            .add(R.id.flMainHolder, PhotoEditorFragment())
            .commit()
    }
}