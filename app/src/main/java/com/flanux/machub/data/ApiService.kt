package com.flanux.machub.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("data/notices.json")
    suspend fun getNotices(): NoticeResponse
    
    @GET("data/downloads.json")
    suspend fun getDownloads(): DownloadResponse
    
    @GET("data/gallery.json")
    suspend fun getGallery(): GalleryResponse
    
    @POST("api/result")
    suspend fun checkResult(@Body request: ResultRequest): ResultResponse
    
    companion object {
        private const val BASE_URL = "https://flanux.github.io/macHub/"
        
        // For result checking, use a different base URL
        private const val RESULT_API_URL = "https://your-server.com/"  // TODO: Deploy Flask API and update this
        
        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(ApiService::class.java)
        }
        
        fun createResultApi(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(RESULT_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(ApiService::class.java)
        }
    }
}
