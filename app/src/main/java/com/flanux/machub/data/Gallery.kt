package com.flanux.machub.data

import com.google.gson.annotations.SerializedName

data class GalleryItem(
    @SerializedName("title")
    val title: String = "",
    
    @SerializedName("image_url")
    val imageUrl: String = "",
    
    @SerializedName("category")
    val category: String = "routine",  // "routine" or "semester_plan"
    
    @SerializedName("scraped_at")
    val scrapedAt: String = ""
)

data class GalleryResponse(
    @SerializedName("gallery")
    val gallery: List<GalleryItem> = emptyList(),
    
    @SerializedName("last_updated")
    val lastUpdated: String = "",
    
    @SerializedName("total_count")
    val totalCount: Int = 0
)
