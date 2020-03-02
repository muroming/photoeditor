package com.muroming.postcardeditor.ui.views.mypictures

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.data.UserPicture
import kotlinx.android.synthetic.main.my_pictures_view.view.*

class UserPicturesView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private lateinit var userPicturesAdapter: UserPicturesAdapter

    init {
        LayoutInflater.from(context).inflate(R.layout.my_pictures_view, this, true)
        initListeners()
        initAdapter()
    }

    private fun initListeners() {
    }

    private fun initAdapter() {
        rvUserPictures
    }

    fun update(pictures: List<UserPicture>) {
        userPicturesAdapter.setItems(pictures)
        userPicturesAdapter.notifyDataSetChanged()
    }
}