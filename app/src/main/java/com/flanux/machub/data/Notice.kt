package com.flanux.machub.data

import com.google.gson.annotations.SerializedName

data class Attachment(
    @SerializedName("label")
    val label: String = "",
    
    @SerializedName("url")
    val url: String = "",
    
    @SerializedName("type")
    val type: String = "link"  // pdf, sharepoint, gdrive, link
)

data class Notice(
    @SerializedName("id")
    val id: String = "",
    
    @SerializedName("title")
    val title: String = "",
    
    @SerializedName("url")
    val url: String = "",
    
    @SerializedName("date_str")
    val dateStr: String = "",
    
    @SerializedName("date_iso")
    val dateIso: String = "",
    
    @SerializedName("category")
    val category: String = "general",  // examination, iost, admission, general
    
    @SerializedName("batches")
    val batches: List<String> = emptyList(),
    
    @SerializedName("semester")
    val semester: String = "",
    
    @SerializedName("year")
    val year: String = "",
    
    @SerializedName("body")
    val body: String = "",
    
    @SerializedName("body_hash")
    val bodyHash: String = "",
    
    @SerializedName("attachments")
    val attachments: List<Attachment> = emptyList()
)

data class NoticeResponse(
    @SerializedName("scraped_at")
    val scrapedAt: String = "",
    
    @SerializedName("count")
    val count: Int = 0,
    
    @SerializedName("new_this_run")
    val newThisRun: Int = 0,
    
    @SerializedName("updated_this_run")
    val updatedThisRun: Int = 0,
    
    @SerializedName("failed_urls")
    val failedUrls: List<String> = emptyList(),
    
    @SerializedName("notices")
    val notices: List<Notice> = emptyList()
)
