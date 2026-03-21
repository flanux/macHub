package com.flanux.machub.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class DataRepository(private val context: Context) {
    private val gson = Gson()
    
    suspend fun loadNotices(): NoticeResponse = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("data/notices.json")
                .bufferedReader()
                .use { it.readText() }
            gson.fromJson(jsonString, NoticeResponse::class.java)
        } catch (e: IOException) {
            e.printStackTrace()
            NoticeResponse()
        }
    }
    
    suspend fun loadDownloads(): DownloadResponse = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("data/downloads.json")
                .bufferedReader()
                .use { it.readText() }
            gson.fromJson(jsonString, DownloadResponse::class.java)
        } catch (e: IOException) {
            e.printStackTrace()
            DownloadResponse()
        }
    }
    
    suspend fun loadGallery(): GalleryResponse = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("data/gallery.json")
                .bufferedReader()
                .use { it.readText() }
            gson.fromJson(jsonString, GalleryResponse::class.java)
        } catch (e: IOException) {
            e.printStackTrace()
            GalleryResponse()
        }
    }
    
    suspend fun loadNews(): NewsResponse = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("data/news.json")
                .bufferedReader()
                .use { it.readText() }
            gson.fromJson(jsonString, NewsResponse::class.java)
        } catch (e: IOException) {
            e.printStackTrace()
            NewsResponse()
        }
    }
}
