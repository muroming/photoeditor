package com.muroming.postcardeditor.ui.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.muroming.postcardeditor.data.UserPicture
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_user_medium_picture.view.*

class UserPicturesAdapter(
    context: Context,
    @LayoutRes private val pictureLayout: Int
) : RecyclerView.Adapter<UserPicturesAdapter.ViewHolder>() {

    private val picasso = Picasso.get()
    private val items: MutableList<UserPicture> = mutableListOf()
    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = inflater.inflate(pictureLayout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.count()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun setItems(newItems: List<UserPicture>) {
        items.apply {
            clear()
            addAll(newItems)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(userPicture: UserPicture) {
            with(itemView) {
                picasso.load(userPicture.uri)
                    .into(ivUserPicture)
            }
        }
    }
}