package com.flanux.machub.data

import com.google.gson.annotations.SerializedName

data class Download(
    @SerializedName("title")
    val title: String = "",
    
    @SerializedName("url")
    val url: String = "",
    
    @SerializedName("type")
    val type: String = "PDF",  // PDF, DOC, XLSX, etc.
    
    @SerializedName("category")
    val category: String = "general",  // "student" or "general"
    
    @SerializedName("scraped_at")
    val scrapedAt: String = ""
)

data class DownloadResponse(
    @SerializedName("downloads")
    val downloads: List<Download> = emptyList(),
    
    @SerializedName("last_updated")
    val lastUpdated: String = "",
    
    @SerializedName("total_count")
    val totalCount: Int = 0
)
