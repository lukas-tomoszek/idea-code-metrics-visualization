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

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.SQLException

class DuckDbServiceTest : BasePlatformTestCase() {

    private lateinit var duckDbService: DuckDbService
    private lateinit var dbFile: File
    private lateinit var queryCacheService: QueryCacheService

    override fun setUp() {
        super.setUp()
        duckDbService = DuckDbService.getInstance(project)
        queryCacheService = QueryCacheService.getInstance(project)
        dbFile = File(project.basePath, ".idea/codeMetricsVisualizations/data.db")

        dbFile.parentFile?.deleteRecursively()
        queryCacheService.clear()
    }

    override fun tearDown() {
        try {
            queryCacheService.clear()
            dbFile.parentFile?.deleteRecursively()
        } finally {
            super.tearDown()
        }
    }

    fun testEnsureDbDirectoryExistsCreatesDirectory() {
        val dbDir = dbFile.parentFile
        dbDir.deleteRecursively()
        assertFalse(dbDir.exists())
        val result = runBlocking {
            duckDbService.executeWriteQuery("CREATE TABLE test_dir_creation (id INTEGER);")
        }
        assertTrue(result.isSuccess)
        assertTrue(dbDir.exists())
        assertTrue(dbDir.isDirectory)
    }

    fun testExecuteWriteQuerySuccessful() = runBlocking {
        val createTableSql = "CREATE TABLE test_write (id INTEGER, name VARCHAR);"
        var result = duckDbService.executeWriteQuery(createTableSql)
        assertTrue("Failed to create table: ${result.exceptionOrNull()?.message}", result.isSuccess)

        val insertSql = "INSERT INTO test_write VALUES (1, 'test_name');"
        result = duckDbService.executeWriteQuery(insertSql)
        assertTrue("Failed to insert data: ${result.exceptionOrNull()?.message}", result.isSuccess)

        val selectResult = duckDbService.executeReadQuery("SELECT * FROM test_write;")
        assertTrue(selectResult.isSuccess)
        assertEquals(1, selectResult.getOrNull()?.rows?.size)
        assertEquals(1, selectResult.getOrNull()?.rows?.first()?.get("id"))
        assertEquals("test_name", selectResult.getOrNull()?.rows?.first()?.get("name"))
    }

    fun testExecuteWriteQueryBlankSqlFails() = runBlocking {
        val result = duckDbService.executeWriteQuery("   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    fun testExecuteWriteQueryInvalidSqlFails() = runBlocking {
        val result = duckDbService.executeWriteQuery("CREATE GIBBERISH test_invalid;")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SQLException)
    }

    fun testExecuteWriteQueryClearsCacheOnSuccess() = runBlocking {
        val selectSql = "SELECT 1 AS val;"
        duckDbService.executeWriteQuery("CREATE TABLE cache_test_table (id INT);").getOrThrow()
        duckDbService.executeReadQuery(selectSql).getOrThrow()
        assertNotNull(queryCacheService.get(selectSql))

        duckDbService.executeWriteQuery("INSERT INTO cache_test_table VALUES(1);").getOrThrow()
        assertNull(queryCacheService.get(selectSql))
    }

    fun testExecuteReadQuerySuccessful() = runBlocking {
        duckDbService.executeWriteQuery("CREATE TABLE test_read (id INTEGER);").getOrThrow()
        duckDbService.executeWriteQuery("INSERT INTO test_read VALUES (42);").getOrThrow()

        val result = duckDbService.executeReadQuery("SELECT id FROM test_read;")
        assertTrue(result.isSuccess)
        val queryResult = result.getOrNull()
        assertNotNull(queryResult)
        assertEquals(listOf("id"), queryResult?.columnNames)
        assertEquals(1, queryResult?.rows?.size)
        assertEquals(42, queryResult?.rows?.first()?.get("id"))
    }

    fun testExecuteReadQueryBlankSqlFails() = runBlocking {
        val result = duckDbService.executeReadQuery(" ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    fun testExecuteReadQueryTableNotFoundFails() = runBlocking {
        val result = duckDbService.executeReadQuery("SELECT * FROM non_existent_table;")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SQLException)
    }

    fun testExecuteReadQueryInvalidSqlFails() = runBlocking {
        duckDbService.executeWriteQuery("CREATE TABLE test_invalid_read (id INTEGER);").getOrThrow()
        val result = duckDbService.executeReadQuery("SELECT FROM test_invalid_read;")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SQLException)
    }

    fun testExecuteReadQueryPopulatesAndUsesCache() = runBlocking {
        duckDbService.executeWriteQuery("CREATE TABLE test_cache_read (data VARCHAR);").getOrThrow()
        duckDbService.executeWriteQuery("INSERT INTO test_cache_read VALUES ('cached_value');").getOrThrow()

        val sql = "SELECT data FROM test_cache_read;"

        queryCacheService.clear()
        assertNull("Cache should be empty initially for this SQL", queryCacheService.get(sql))

        val firstResult = duckDbService.executeReadQuery(sql)
        assertTrue("First query failed", firstResult.isSuccess)
        assertEquals("cached_value", firstResult.getOrNull()?.rows?.first()?.get("data"))
        assertNotNull("Cache should be populated after first query", queryCacheService.get(sql))
        assertEquals(firstResult, queryCacheService.get(sql))

        FileUtil.delete(dbFile)

        val cachedResult = duckDbService.executeReadQuery(sql)
        assertTrue("Cached query failed", cachedResult.isSuccess)
        assertEquals("cached_value", cachedResult.getOrNull()?.rows?.first()?.get("data"))
    }

    fun testExecuteReadQueryFailsIfDbFileDoesNotExistForReadOnly() = runBlocking {
        assertFalse("DB file should not exist before any write", dbFile.exists())
        val result = duckDbService.executeReadQuery("SELECT 1;")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SQLException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Database file does not exist") == true)
    }

    fun testMultipleReadQueries() = runBlocking {
        duckDbService.executeWriteQuery("CREATE TABLE t1 (id INT); INSERT INTO t1 VALUES (1);").getOrThrow()
        duckDbService.executeWriteQuery("CREATE TABLE t2 (name VARCHAR); INSERT INTO t2 VALUES ('hello');").getOrThrow()

        val res1 = duckDbService.executeReadQuery("SELECT * FROM t1;")
        assertTrue(res1.isSuccess)
        assertEquals(1, res1.getOrNull()?.rows?.first()?.get("id"))

        val res2 = duckDbService.executeReadQuery("SELECT * FROM t2;")
        assertTrue(res2.isSuccess)
        assertEquals("hello", res2.getOrNull()?.rows?.first()?.get("name"))
    }

    fun testQueryResultStructure() = runBlocking {
        val createSql = """
            CREATE TABLE result_structure (
                col_int INTEGER,
                col_varchar VARCHAR,
                col_double DOUBLE,
                col_bool BOOLEAN,
                col_date DATE
            );
        """
        duckDbService.executeWriteQuery(createSql).getOrThrow()
        val insertSql = """
            INSERT INTO result_structure VALUES 
            (1, 'text', 3.14, true, '2024-01-15'),
            (NULL, NULL, NULL, NULL, NULL);
        """
        duckDbService.executeWriteQuery(insertSql).getOrThrow()

        val result = duckDbService.executeReadQuery("SELECT * FROM result_structure ORDER BY col_int DESC NULLS LAST;")
        assertTrue(result.isSuccess)
        val qr = result.getOrNull()!!

        assertEquals(listOf("col_int", "col_varchar", "col_double", "col_bool", "col_date"), qr.columnNames)

        assertEquals(listOf("INTEGER", "VARCHAR", "DOUBLE", "BOOLEAN", "DATE"), qr.columnTypes.map { it.uppercase() })

        assertEquals(2, qr.rows.size)

        val firstRow = qr.rows[0]
        assertEquals(1, firstRow["col_int"])
        assertEquals("text", firstRow["col_varchar"])
        assertEquals(3.14, firstRow["col_double"])
        assertEquals(true, firstRow["col_bool"])
        assertNotNull(firstRow["col_date"])

        val secondRow = qr.rows[1]
        assertNull(secondRow["col_int"])
        assertNull(secondRow["col_varchar"])
        assertNull(secondRow["col_double"])
        assertNull(secondRow["col_bool"])
        assertNull(secondRow["col_date"])
    }
}
