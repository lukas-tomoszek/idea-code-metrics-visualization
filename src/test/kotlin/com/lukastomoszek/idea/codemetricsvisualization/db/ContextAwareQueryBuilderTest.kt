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

import com.lukastomoszek.idea.codemetricsvisualization.context.model.ContextInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextAwareQueryBuilderTest {

    private val emptyContext = ContextInfo(null, null, emptyList(), emptyList(), null, null, emptyList(), emptyList())
    private val fullContext = ContextInfo(
        methodFqn = "com.example.MyClass.myMethod",
        featureName = "my-feature",
        allMethodsInFile = listOf("com.example.MyClass.myMethod", "com.example.MyClass.otherMethod"),
        allFeaturesInFile = listOf("my-feature", "other-feature"),
        mappingPath = "/api/users/[^/]+",
        mappingMethod = "GET",
        allMappingPathsInFile = listOf("/api/users/[^/]+", "/api/orders"),
        allMappingMethodsInFile = listOf("GET", "POST")
    )

    @Test
    fun buildQueryReplacesMethodFqnPlaceholder() {
        val sql = "SELECT * FROM logs WHERE method = '${ContextAwareQueryBuilder.METHOD_FQN_PLACEHOLDER}'"
        val result = ContextAwareQueryBuilder.buildQuery(sql, fullContext)
        assertEquals(Result.success("SELECT * FROM logs WHERE method = 'com.example.MyClass.myMethod'"), result)
    }

    @Test
    fun buildQueryMethodFqnPlaceholderWithNullContextAndDefault() {
        val sql = "SELECT * FROM logs WHERE method = '${ContextAwareQueryBuilder.METHOD_FQN_PLACEHOLDER}'"
        val result = ContextAwareQueryBuilder.buildQuery(sql, emptyContext, useDefaultsForUnresolved = true)
        assertEquals(
            Result.success("SELECT * FROM logs WHERE method = '${ContextAwareQueryBuilder.DEFAULT_PLACEHOLDER_VALUE}'"),
            result
        )
    }

    @Test
    fun buildQueryMethodFqnPlaceholderWithNullContextAndNoDefaultFails() {
        val sql = "SELECT * FROM logs WHERE method = '${ContextAwareQueryBuilder.METHOD_FQN_PLACEHOLDER}'"
        val result = ContextAwareQueryBuilder.buildQuery(sql, emptyContext, useDefaultsForUnresolved = false)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals("Missing required method FQN for SQL template", result.exceptionOrNull()?.message)
    }

    @Test
    fun buildQueryReplacesFeatureNamePlaceholder() {
        val sql = "SELECT * FROM features WHERE name = '${ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER}'"
        val result = ContextAwareQueryBuilder.buildQuery(sql, fullContext)
        assertEquals(Result.success("SELECT * FROM features WHERE name = 'my-feature'"), result)
    }

    @Test
    fun buildQueryFeatureNamePlaceholderWithNullContextAndDefault() {
        val sql = "SELECT * FROM features WHERE name = '${ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER}'"
        val result = ContextAwareQueryBuilder.buildQuery(sql, emptyContext, useDefaultsForUnresolved = true)
        assertEquals(
            Result.success("SELECT * FROM features WHERE name = '${ContextAwareQueryBuilder.DEFAULT_PLACEHOLDER_VALUE}'"),
            result
        )
    }

    @Test
    fun buildQueryFeatureNamePlaceholderWithNullContextAndNoDefaultFails() {
        val sql = "SELECT * FROM features WHERE name = '${ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER}'"
        val result = ContextAwareQueryBuilder.buildQuery(sql, emptyContext, useDefaultsForUnresolved = false)
        assertTrue(result.isFailure)
        assertEquals("Missing required feature name for SQL template", result.exceptionOrNull()?.message)
    }

    @Test
    fun buildQueryReplacesMethodFqnsInFilePlaceholder() {
        val sql = "SELECT * FROM methods WHERE fqn IN (${ContextAwareQueryBuilder.METHOD_FQNS_IN_FILE_PLACEHOLDER})"
        val expectedSql =
            "SELECT * FROM methods WHERE fqn IN ('com.example.MyClass.myMethod', 'com.example.MyClass.otherMethod')"
        val result = ContextAwareQueryBuilder.buildQuery(sql, fullContext)
        assertEquals(Result.success(expectedSql), result)
    }

    @Test
    fun buildQueryMethodFqnsInFilePlaceholderWithEmptyList() {
        val sql = "SELECT * FROM methods WHERE fqn IN (${ContextAwareQueryBuilder.METHOD_FQNS_IN_FILE_PLACEHOLDER})"
        val result = ContextAwareQueryBuilder.buildQuery(sql, emptyContext)
        assertEquals(Result.success("SELECT * FROM methods WHERE fqn IN (NULL)"), result)
    }

    @Test
    fun buildQueryReplacesFeatureNamesInFilePlaceholder() {
        val sql = "SELECT * FROM features WHERE name IN (${ContextAwareQueryBuilder.FEATURE_NAMES_IN_FILE_PLACEHOLDER})"
        val expectedSql = "SELECT * FROM features WHERE name IN ('my-feature', 'other-feature')"
        val result = ContextAwareQueryBuilder.buildQuery(sql, fullContext)
        assertEquals(Result.success(expectedSql), result)
    }

    @Test
    fun buildQueryReplacesMappingPathPlaceholder() {
        val sql = "SELECT * FROM routes WHERE path_regex = '${ContextAwareQueryBuilder.MAPPING_PATH_PLACEHOLDER}'"
        val result = ContextAwareQueryBuilder.buildQuery(sql, fullContext)
        assertEquals(Result.success("SELECT * FROM routes WHERE path_regex = '/api/users/[^/]+'"), result)
    }

    @Test
    fun buildQueryMappingPathPlaceholderWithNullContextAndDefault() {
        val sql = "SELECT * FROM routes WHERE path_regex = '${ContextAwareQueryBuilder.MAPPING_PATH_PLACEHOLDER}'"
        val result = ContextAwareQueryBuilder.buildQuery(sql, emptyContext, useDefaultsForUnresolved = true)
        assertEquals(
            Result.success("SELECT * FROM routes WHERE path_regex = '${ContextAwareQueryBuilder.DEFAULT_REGEX_PLACEHOLDER_VALUE}'"),
            result
        )
    }

    @Test
    fun buildQueryMappingPathPlaceholderWithNullContextAndNoDefaultFails() {
        val sql = "SELECT * FROM routes WHERE path_regex = '${ContextAwareQueryBuilder.MAPPING_PATH_PLACEHOLDER}'"
        val result = ContextAwareQueryBuilder.buildQuery(sql, emptyContext, useDefaultsForUnresolved = false)
        assertTrue(result.isFailure)
        assertEquals("Missing required mapping path for SQL template", result.exceptionOrNull()?.message)
    }

    @Test
    fun buildQueryReplacesMappingMethodPlaceholder() {
        val sql = "SELECT * FROM routes WHERE http_method = '${ContextAwareQueryBuilder.MAPPING_METHOD_PLACEHOLDER}'"
        val result = ContextAwareQueryBuilder.buildQuery(sql, fullContext)
        assertEquals(Result.success("SELECT * FROM routes WHERE http_method = 'GET'"), result)
    }

    @Test
    fun buildQueryReplacesMappingPathsInFilePlaceholder() {
        val sql =
            "SELECT * FROM routes WHERE path_regex IN ('${ContextAwareQueryBuilder.MAPPING_PATHS_IN_FILE_PLACEHOLDER}')"
        val expectedSql = "SELECT * FROM routes WHERE path_regex IN ('/api/users/[^/]+|/api/orders')"
        val result = ContextAwareQueryBuilder.buildQuery(sql, fullContext)
        assertEquals(Result.success(expectedSql), result)
    }

    @Test
    fun buildQueryMappingPathsInFilePlaceholderWithEmptyList() {
        val sql =
            "SELECT * FROM routes WHERE path_regex IN ('${ContextAwareQueryBuilder.MAPPING_PATHS_IN_FILE_PLACEHOLDER}')"
        val result = ContextAwareQueryBuilder.buildQuery(sql, emptyContext)
        assertEquals(Result.success("SELECT * FROM routes WHERE path_regex IN ('NULL')"), result)
    }

    @Test
    fun buildQueryReplacesMappingMethodsInFilePlaceholder() {
        val sql =
            "SELECT * FROM routes WHERE http_method IN (${ContextAwareQueryBuilder.MAPPING_METHODS_IN_FILE_PLACEHOLDER})"
        val expectedSql = "SELECT * FROM routes WHERE http_method IN ('GET', 'POST')"
        val result = ContextAwareQueryBuilder.buildQuery(sql, fullContext)
        assertEquals(Result.success(expectedSql), result)
    }

    @Test
    fun buildQueryAllPlaceholdersReplaced() {
        val sql = """
            SELECT COUNT(*)
            FROM logs
            WHERE method_fqn = '${ContextAwareQueryBuilder.METHOD_FQN_PLACEHOLDER}'
              AND feature_name = '${ContextAwareQueryBuilder.FEATURE_NAME_PLACEHOLDER}'
              AND file_methods IN (${ContextAwareQueryBuilder.METHOD_FQNS_IN_FILE_PLACEHOLDER})
              AND file_features IN (${ContextAwareQueryBuilder.FEATURE_NAMES_IN_FILE_PLACEHOLDER})
              AND mapping_path_regex = '${ContextAwareQueryBuilder.MAPPING_PATH_PLACEHOLDER}'
              AND http_method = '${ContextAwareQueryBuilder.MAPPING_METHOD_PLACEHOLDER}'
              AND file_mappings_regex = '${ContextAwareQueryBuilder.MAPPING_PATHS_IN_FILE_PLACEHOLDER}'
              AND file_http_methods IN (${ContextAwareQueryBuilder.MAPPING_METHODS_IN_FILE_PLACEHOLDER});
        """.trimIndent()

        val expected = """
            SELECT COUNT(*)
            FROM logs
            WHERE method_fqn = 'com.example.MyClass.myMethod'
              AND feature_name = 'my-feature'
              AND file_methods IN ('com.example.MyClass.myMethod', 'com.example.MyClass.otherMethod')
              AND file_features IN ('my-feature', 'other-feature')
              AND mapping_path_regex = '/api/users/[^/]+'
              AND http_method = 'GET'
              AND file_mappings_regex = '/api/users/[^/]+|/api/orders'
              AND file_http_methods IN ('GET', 'POST');
        """.trimIndent()
        val result = ContextAwareQueryBuilder.buildQuery(sql, fullContext)
        assertEquals(Result.success(expected), result)
    }

    @Test
    fun buildQueryNoPlaceholdersPresentReturnsOriginalSql() {
        val sql = "SELECT 1 FROM DUAL;"
        val result = ContextAwareQueryBuilder.buildQuery(sql, fullContext)
        assertEquals(Result.success(sql), result)
    }

    @Test
    fun buildQuerySqlInjectionInListFormatting() {
        val contextWithQuotes = emptyContext.copy(
            allMethodsInFile = listOf("testMethod", "another'Method")
        )
        val sql = "SELECT * FROM methods WHERE fqn IN (${ContextAwareQueryBuilder.METHOD_FQNS_IN_FILE_PLACEHOLDER})"
        val expectedSql = "SELECT * FROM methods WHERE fqn IN ('testMethod', 'another''Method')"
        val result = ContextAwareQueryBuilder.buildQuery(sql, contextWithQuotes)
        assertEquals(Result.success(expectedSql), result)
    }

    @Test
    fun buildQueryWithOnlySomePlaceholdersNeededAndUseDefaultsForUnresolvedTrue() {
        val sql =
            "SELECT '${ContextAwareQueryBuilder.METHOD_FQN_PLACEHOLDER}', '${ContextAwareQueryBuilder.MAPPING_PATH_PLACEHOLDER}'"
        val partialContext = emptyContext.copy(methodFqn = "com.example.Partial")

        val expectedSql = "SELECT 'com.example.Partial', '${ContextAwareQueryBuilder.DEFAULT_REGEX_PLACEHOLDER_VALUE}'"
        val result = ContextAwareQueryBuilder.buildQuery(sql, partialContext, useDefaultsForUnresolved = true)
        assertEquals(Result.success(expectedSql), result)
    }

    @Test
    fun buildQueryWithOnlySomePlaceholdersNeededAndUseDefaultsForUnresolvedFalse() {
        val sql =
            "SELECT '${ContextAwareQueryBuilder.METHOD_FQN_PLACEHOLDER}', '${ContextAwareQueryBuilder.MAPPING_PATH_PLACEHOLDER}'"
        val partialContext = emptyContext.copy(methodFqn = "com.example.Partial")

        val result = ContextAwareQueryBuilder.buildQuery(sql, partialContext, useDefaultsForUnresolved = false)
        assertTrue(result.isFailure)
        assertEquals("Missing required mapping path for SQL template", result.exceptionOrNull()?.message)
    }
}
