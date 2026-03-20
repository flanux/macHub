package com.flanux.machub.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * TU Result Scraper - Device-side scraping from tuexam.edu.np
 * No backend needed - everything runs on the device
 * 
 * Note: SSL verification disabled because TU's certificate is expired
 */
class TUResultScraper {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .apply {
            // Bypass SSL verification (TU's certificate is expired)
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            hostnameVerifier { _, _ -> true }
        }
        .build()
    
    // Try multiple TU portal URLs (fallback support)
    private val tuPortalUrls = listOf(
        "https://tuexam.edu.np/ExamResultView.aspx",
        "http://www.tuexam.edu.np/ExamResultView.aspx",
        "https://exam.tu.edu.np/ExamResultView.aspx"
    )
    
    /**
     * Main function to fetch student result
     * Returns StudentResult on success, null on failure
     */
    suspend fun fetchResult(
        symbolNumber: String,
        dateOfBirth: String, // Format: YYYY-MM-DD
        semester: Int? = null
    ): ResultResponse = withContext(Dispatchers.IO) {
        try {
            // Try each URL until one works
            for (url in tuPortalUrls) {
                try {
                    val result = fetchFromUrl(url, symbolNumber, dateOfBirth, semester)
                    if (result.success) {
                        return@withContext result
                    }
                } catch (e: Exception) {
                    // Try next URL
                    continue
                }
            }
            
            // All URLs failed
            ResultResponse(
                success = false,
                message = "Could not connect to TU portal. All URLs failed.",
                result = null
            )
            
        } catch (e: Exception) {
            ResultResponse(
                success = false,
                message = "Error: ${e.message}",
                result = null
            )
        }
    }
    
    /**
     * Fetch result from a specific URL
     */
    private fun fetchFromUrl(
        url: String,
        symbolNumber: String,
        dateOfBirth: String,
        semester: Int?
    ): ResultResponse {
        // Step 1: Get the form page to extract ViewState
        val getRequest = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            .build()
        
        val getResponse = client.newCall(getRequest).execute()
        if (!getResponse.isSuccessful) {
            throw Exception("Failed to load form page: ${getResponse.code}")
        }
        
        val formHtml = getResponse.body?.string() ?: throw Exception("Empty response")
        val formDoc = Jsoup.parse(formHtml)
        
        // Step 2: Extract ASP.NET ViewState tokens (required for form submission)
        val viewState = formDoc.select("input[name=__VIEWSTATE]").attr("value")
        val viewStateGenerator = formDoc.select("input[name=__VIEWSTATEGENERATOR]").attr("value")
        val eventValidation = formDoc.select("input[name=__EVENTVALIDATION]").attr("value")
        
        if (viewState.isEmpty()) {
            throw Exception("Could not extract ViewState from form")
        }
        
        // Step 3: Convert DOB format (YYYY-MM-DD → MM/DD/YYYY)
        val dobFormatted = formatDob(dateOfBirth)
        
        // Step 4: Build POST request
        val formBody = FormBody.Builder()
            .add("__VIEWSTATE", viewState)
            .add("__VIEWSTATEGENERATOR", viewStateGenerator)
            .add("__EVENTVALIDATION", eventValidation)
            .add("txtSymbol", symbolNumber)
            .add("txtDOB", dobFormatted)
            .add("btnSubmit", "Show Result")
            .build()
        
        val postRequest = Request.Builder()
            .url(url)
            .post(formBody)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()
        
        val postResponse = client.newCall(postRequest).execute()
        if (!postResponse.isSuccessful) {
            throw Exception("Failed to submit form: ${postResponse.code}")
        }
        
        val resultHtml = postResponse.body?.string() ?: throw Exception("Empty result")
        val resultDoc = Jsoup.parse(resultHtml)
        
        // Step 5: Parse the result
        return parseResult(resultDoc, symbolNumber)
    }
    
    /**
     * Parse HTML result page
     */
    private fun parseResult(doc: Document, symbolNumber: String): ResultResponse {
        // Check for error messages
        val errorDiv = doc.select("div.alert-danger, span.error").firstOrNull()
        if (errorDiv != null) {
            val errorMsg = errorDiv.text().trim()
            if (errorMsg.isNotEmpty()) {
                return ResultResponse(
                    success = false,
                    message = errorMsg,
                    result = null
                )
            }
        }
        
        // Find result table
        val resultTable = doc.select("table#grdResult, table#GridView1, table.table").firstOrNull()
        if (resultTable == null) {
            // Check if page says "no result found"
            val pageText = doc.text().lowercase()
            return if ("no result" in pageText || "not found" in pageText) {
                ResultResponse(
                    success = false,
                    message = "No result found for the given credentials. Please verify symbol number and date of birth.",
                    result = null
                )
            } else {
                ResultResponse(
                    success = false,
                    message = "Unable to parse result. Portal format may have changed.",
                    result = null
                )
            }
        }
        
        // Extract student info
        val studentName = extractText(doc, listOf(
            "lblStudentName", "lblName", "StudentName",
            "ctl00_ContentPlaceHolder1_lblStudentName"
        )) ?: "Student Name"
        
        val program = extractText(doc, listOf(
            "lblProgram", "lblFaculty", "Program",
            "ctl00_ContentPlaceHolder1_lblProgram"
        )) ?: "B.Sc. CSIT"
        
        val sgpa = extractText(doc, listOf(
            "lblSGPA", "SGPA", "SemesterGPA",
            "ctl00_ContentPlaceHolder1_lblSGPA"
        )) ?: "0.00"
        
        val cgpa = extractText(doc, listOf(
            "lblCGPA", "CGPA", "CumulativeGPA",
            "ctl00_ContentPlaceHolder1_lblCGPA"
        ))
        
        val semester = extractText(doc, listOf(
            "lblSemester", "Semester",
            "ctl00_ContentPlaceHolder1_lblSemester"
        )) ?: "1"
        
        val year = extractText(doc, listOf(
            "lblYear", "Year", "ExamYear",
            "ctl00_ContentPlaceHolder1_lblYear"
        )) ?: "2079"
        
        // Parse subjects from table
        val subjects = parseSubjectTable(resultTable)
        
        if (subjects.isEmpty()) {
            return ResultResponse(
                success = false,
                message = "Result table found but no subjects parsed. Portal format may have changed.",
                result = null
            )
        }
        
        // Determine pass/fail status
        val status = determineStatus(subjects)
        
        val result = StudentResult(
            symbolNumber = symbolNumber,
            name = studentName,
            semester = semester,
            program = program,
            subjects = subjects,
            sgpa = sgpa,
            cgpa = cgpa,
            status = status,
            year = year,
            fetchedAt = System.currentTimeMillis()
        )
        
        return ResultResponse(
            success = true,
            message = "Result fetched successfully",
            result = result
        )
    }
    
    /**
     * Parse subject table
     */
    private fun parseSubjectTable(table: org.jsoup.nodes.Element): List<SubjectResult> {
        val subjects = mutableListOf<SubjectResult>()
        val rows = table.select("tr")
        
        // Skip header row(s)
        val dataRows = rows.filter { row ->
            row.select("th").isEmpty() // Skip rows with th elements
        }
        
        for (row in dataRows) {
            val cols = row.select("td")
            if (cols.size < 4) continue // Need at least code, name, grade
            
            val colTexts = cols.map { it.text().trim() }
            
            // Skip empty or header-like rows
            if (colTexts[0].lowercase() in listOf("sn", "s.n.", "code", "subject", "")) {
                continue
            }
            
            // Parse based on column count
            val subject = when {
                colTexts.size >= 7 -> SubjectResult(
                    code = colTexts[0],
                    name = colTexts[1],
                    creditHour = colTexts[2],
                    gradePoint = colTexts[3],
                    grade = colTexts[4],
                    marks = colTexts.getOrNull(5),
                    remarks = colTexts.getOrNull(6)
                )
                colTexts.size >= 5 -> SubjectResult(
                    code = colTexts[0],
                    name = colTexts[1],
                    creditHour = colTexts[2],
                    grade = colTexts[3],
                    gradePoint = colTexts.getOrNull(4) ?: "0.0",
                    marks = colTexts.getOrNull(5),
                    remarks = null
                )
                else -> continue
            }
            
            if (subject.code.isNotEmpty() && subject.name.isNotEmpty()) {
                subjects.add(subject)
            }
        }
        
        return subjects
    }
    
    /**
     * Extract text from element by trying multiple IDs
     */
    private fun extractText(doc: Document, ids: List<String>): String? {
        for (id in ids) {
            // Try by ID
            val element = doc.select("#$id").firstOrNull()
            if (element != null) {
                val text = element.text().trim()
                if (text.isNotEmpty()) return text
            }
            
            // Try by name attribute
            val nameElement = doc.select("[name=$id]").firstOrNull()
            if (nameElement != null) {
                val text = nameElement.text().trim()
                if (text.isNotEmpty()) return text
            }
        }
        return null
    }
    
    /**
     * Determine overall pass/fail status
     */
    private fun determineStatus(subjects: List<SubjectResult>): String {
        for (subject in subjects) {
            val grade = subject.grade.uppercase()
            val remarks = subject.remarks?.lowercase() ?: ""
            
            // Check for fail indicators
            if (grade in listOf("F", "NG", "ABS", "EX")) {
                return "Fail"
            }
            if ("fail" in remarks || "back" in remarks || "abs" in remarks) {
                return remarks.replaceFirstChar { it.uppercase() }
            }
        }
        return "Pass"
    }
    
    /**
     * Convert YYYY-MM-DD to MM/DD/YYYY (TU portal format)
     */
    private fun formatDob(dob: String): String {
        if ("-" !in dob) return dob
        
        val parts = dob.split("-")
        if (parts.size != 3) return dob
        
        val (year, month, day) = parts
        return "$month/$day/$year"
    }
}
