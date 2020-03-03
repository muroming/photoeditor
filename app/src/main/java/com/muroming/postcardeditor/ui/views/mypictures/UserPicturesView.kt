package com.muroming.postcardeditor.ui.views.mypictures

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.muroming.postcardeditor.R
import com.muroming.postcardeditor.data.UserPicture
import com.muroming.postcardeditor.ui.views.UserPicturesAdapter
import kotlinx.android.synthetic.main.my_pictures_view.view.*

class UserPicturesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val userPicturesAdapter: UserPicturesAdapter by lazy {
        UserPicturesAdapter(context, R.layout.item_user_medium_picture)
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.my_pictures_view, this, true)
        initListeners()
        initAdapter()
    }

    private fun initListeners() {

    }

    private fun initAdapter() {
        rvUserPictures.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = userPicturesAdapter
        }
    }

    fun update(pictures: List<UserPicture>) {
        userPicturesAdapter.setItems(pictures)
        userPicturesAdapter.notifyDataSetChanged()
    }
}