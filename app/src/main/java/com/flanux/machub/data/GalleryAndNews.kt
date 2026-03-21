package com.flanux.machub.data

import com.google.gson.annotations.SerializedName

// GALLERY
data class GalleryItem(
    @SerializedName("title")
    val title: String = "",
    
    @SerializedName("type")
    val type: String = "",  // "album" or "webview"
    
    @SerializedName("url")
    val url: String = "",
    
    @SerializedName("thumbnail")
    val thumbnail: String? = null,
    
    @SerializedName("batches")
    val batches: List<String> = emptyList()
)

data class GalleryResponse(
    @SerializedName("scraped_at")
    val scrapedAt: String = "",
    
    @SerializedName("count")
    val count: Int = 0,
    
    @SerializedName("note")
    val note: String = "",
    
    @SerializedName("items")
    val items: List<GalleryItem> = emptyList()
)

// NEWS
data class NewsItem(
    @SerializedName("id")
    val id: String = "",
    
    @SerializedName("title")
    val title: String = "",
    
    @SerializedName("url")
    val url: String = "",
    
    @SerializedName("thumbnail")
    val thumbnail: String = "",
    
    @SerializedName("date_str")
    val dateStr: String = "",
    
    @SerializedName("date_iso")
    val dateIso: String = "",
    
    @SerializedName("batches")
    val batches: List<String> = emptyList()
)

data class NewsResponse(
    @SerializedName("scraped_at")
    val scrapedAt: String = "",
    
    @SerializedName("count")
    val count: Int = 0,
    
    @SerializedName("items")
    val items: List<NewsItem> = emptyList()
)
