package com.flanux.machub.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface ApiService {
    @GET("data/notices.json")
    suspend fun getNotices(): NoticeResponse
    
    @GET("data/downloads.json")
    suspend fun getDownloads(): DownloadResponse
    
    @GET("data/gallery.json")
    suspend fun getGallery(): GalleryResponse
    
    companion object {
        private const val BASE_URL = "https://flanux.github.io/macHub/"
        
        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(ApiService::class.java)
        }
    }
}
