package com.lukastomoszek.idea.codemetricsvisualization.db

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.lukastomoszek.idea.codemetricsvisualization.db.model.QueryResult
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class QueryCacheService {
    private val cache = ConcurrentHashMap<String, Result<QueryResult>>()

    fun get(sql: String): Result<QueryResult>? {
        thisLogger().debug("Cache hit SQL: '$sql' value: ${cache[sql]?.getOrNull()}")
        return cache[sql]
    }

    fun put(sql: String, result: Result<QueryResult>) {
        cache[sql] = result
    }

    fun clear() {
        cache.clear()
        thisLogger().info("Cache cleared.")
    }

    companion object {
        fun getInstance(project: Project): QueryCacheService =
            project.getService(QueryCacheService::class.java)
    }
}
