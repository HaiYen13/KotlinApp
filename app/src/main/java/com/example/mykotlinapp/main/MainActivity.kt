package com.example.mykotlinapp.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.mykotlinapp.R
import com.example.mykotlinapp.download.DownloadFragment
import com.example.mykotlinapp.favorite.FavoriteFragment
import com.example.mykotlinapp.home.HomeFragment

import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {
    var homeFragment: Fragment? = null
    var favoriteFragment: Fragment? = null
    var downloadFragment: Fragment? = null
    var navigationBarView: BottomNavigationView ?= null
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        initAction()
    }
    private fun initView() {
        homeFragment = HomeFragment()
        downloadFragment = DownloadFragment()
        favoriteFragment = FavoriteFragment()
        navigationBarView = findViewById(R.id.bottomNavi)
        supportFragmentManager.beginTransaction().replace(R.id.fragmentManager,
            homeFragment as HomeFragment).commit()
    }
    private fun initAction() {
        navigationBarView?.setOnItemSelectedListener { item ->
            val selectedFragment = when(item.itemId){
                R.id.action_home -> homeFragment
                R.id.action_favorite -> favoriteFragment
                R.id.action_download -> downloadFragment
                else -> homeFragment
            }
            selectedFragment?.let {
                supportFragmentManager.beginTransaction().replace(R.id.fragmentManager,
                    it).commit()
            }
            true
        }
    }
}
