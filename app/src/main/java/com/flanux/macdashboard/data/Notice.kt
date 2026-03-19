package com.flanux.macdashboard.data

import com.google.gson.annotations.SerializedName

data class Notice(
    @SerializedName("id")
    val id: Int = 0,
    
    @SerializedName("title")
    val title: String = "",
    
    @SerializedName("url")
    val url: String = "",
    
    @SerializedName("category")
    val category: String = "general",
    
    @SerializedName("batch")
    val batch: String? = null,
    
    @SerializedName("scraped_at")
    val scrapedAt: String = ""
)

data class NoticeResponse(
    @SerializedName("notices")
    val notices: List<Notice> = emptyList(),
    
    @SerializedName("last_updated")
    val lastUpdated: String = "",
    
    @SerializedName("total_count")
    val totalCount: Int = 0
)
