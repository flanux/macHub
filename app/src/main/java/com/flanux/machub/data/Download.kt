package com.flanux.machub.data

import com.google.gson.annotations.SerializedName

data class Download(
    @SerializedName("section")
    val section: String = "",  // "student" or "general"
    
    @SerializedName("program")
    val program: String = "",  // B.Sc.CSIT, BCA, etc.
    
    @SerializedName("level")
    val level: String = "",  // Sem-I, Sem-II, etc.
    
    @SerializedName("semester")
    val semester: String = "",  // 1, 2, 3...
    
    @SerializedName("type")
    val type: String = "",  // E-Books, Notes, Syllabus, Question Collection
    
    @SerializedName("url")
    val url: String = "",
    
    @SerializedName("att_type")
    val attType: String = ""  // gdrive, sharepoint, pdf, link
)

data class DownloadResponse(
    @SerializedName("scraped_at")
    val scrapedAt: String = "",
    
    @SerializedName("count")
    val count: Int = 0,
    
    @SerializedName("student_count")
    val studentCount: Int = 0,
    
    @SerializedName("general_count")
    val generalCount: Int = 0,
    
    @SerializedName("downloads")
    val downloads: List<Download> = emptyList()
)
