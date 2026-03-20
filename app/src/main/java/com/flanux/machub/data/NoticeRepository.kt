package com.flanux.machub.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.InputStreamReader

class NoticeRepository {

    // Reads notices.json from the local 'data/' folder
    private fun loadNotices(): List<Notice> {
        return try {
            val inputStream = File("data/notices.json").inputStream()
            InputStreamReader(inputStream).use { reader ->
                val type = object : TypeToken<List<Notice>>() {}.type
                Gson().fromJson<List<Notice>>(reader, type)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getNotices(): Flow<Result<List<Notice>>> = flow {
        emit(Result.success(loadNotices()))
    }

    fun getNoticesByCategory(category: String): Flow<Result<List<Notice>>> = flow {
        val filtered = loadNotices().filter { it.category == category }
        emit(Result.success(filtered))
    }

    fun getNoticesByBatch(batch: String): Flow<Result<List<Notice>>> = flow {
        val filtered = loadNotices().filter { it.batch == batch }
        emit(Result.success(filtered))
    }
}
