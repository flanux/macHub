package com.flanux.machub.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NoticeRepository(
    private val apiService: ApiService = ApiService.create()
) {
    
    fun getNotices(): Flow<Result<List<Notice>>> = flow {
        try {
            val response = apiService.getNotices()
            emit(Result.success(response.notices))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    fun getNoticesByCategory(category: String): Flow<Result<List<Notice>>> = flow {
        try {
            val response = apiService.getNotices()
            val filtered = response.notices.filter { it.category == category }
            emit(Result.success(filtered))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    fun getNoticesByBatch(batch: String): Flow<Result<List<Notice>>> = flow {
        try {
            val response = apiService.getNotices()
            val filtered = response.notices.filter { it.batch == batch }
            emit(Result.success(filtered))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
