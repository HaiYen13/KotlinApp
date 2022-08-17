package com.example.mykotlinapp.detail

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.example.mykotlinapp.model.ImageModel
import com.github.chrisbanes.photoview.PhotoView
import com.squareup.picasso.Picasso

class ViewPagerAdapter(private val context: Context?, private val list: ArrayList<ImageModel>?) : PagerAdapter() {
    var imageView : PhotoView ?= null
    override fun getCount(): Int {
        return list?.size ?: 0
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        // on below line we are removing view
        container.removeView(`object` as View)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        imageView = PhotoView(context)
        Picasso.get()
            .load(list?.get(position)?.url)
            .into(imageView)

        container.addView(imageView)
        return imageView as PhotoView
    }
}