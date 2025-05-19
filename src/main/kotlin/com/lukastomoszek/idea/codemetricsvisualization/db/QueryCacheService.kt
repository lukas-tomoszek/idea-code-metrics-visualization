/*
 * Copyright (c) 2025 Lukáš Tomoszek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
