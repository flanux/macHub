package com.flanux.machub.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface ApiService {
    @GET("data/notices.json")
    suspend fun getNotices(): NoticeResponse
    
    companion object {
        // TODO: Replace USERNAME and REPO with your actual GitHub username and repo name
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
