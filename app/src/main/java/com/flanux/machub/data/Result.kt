package com.flanux.machub.data

import com.google.gson.annotations.SerializedName

data class StudentResult(
    @SerializedName("symbol_number")
    val symbolNumber: String = "",
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("semester")
    val semester: String = "",
    
    @SerializedName("program")
    val program: String = "B.Sc. CSIT",
    
    @SerializedName("subjects")
    val subjects: List<SubjectResult> = emptyList(),
    
    @SerializedName("sgpa")
    val sgpa: String = "",
    
    @SerializedName("cgpa")
    val cgpa: String? = null,
    
    @SerializedName("status")
    val status: String = "",  // "Pass" or "Fail" or "Back"
    
    @SerializedName("year")
    val year: String = "",
    
    @SerializedName("fetched_at")
    val fetchedAt: Long = System.currentTimeMillis()  // Timestamp for caching
)

data class SubjectResult(
    @SerializedName("code")
    val code: String = "",
    
    @SerializedName("name")
    val name: String = "",
    
    @SerializedName("credit_hour")
    val creditHour: String = "",
    
    @SerializedName("grade_point")
    val gradePoint: String = "",
    
    @SerializedName("grade")
    val grade: String = "",
    
    @SerializedName("marks")
    val marks: String? = null,
    
    @SerializedName("remarks")
    val remarks: String? = null
)

data class ResultRequest(
    val symbolNumber: String,
    val dateOfBirth: String,  // Format: YYYY-MM-DD
    val semester: Int? = null
)

data class ResultResponse(
    @SerializedName("success")
    val success: Boolean = false,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("result")
    val result: StudentResult? = null
)
