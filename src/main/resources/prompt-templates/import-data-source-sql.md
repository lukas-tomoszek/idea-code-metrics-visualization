# Generate DuckDB SQL for Data Source Import

**Task:**
Generate a DuckDB SQL query template suitable for importing data into a DuckDB database.
The SQL query must:

1. Read the file with the correct reader (`read_csv`, `read_parquet`, `read_json`, etc.)
2. Parses timestamps and fields (`to_timestamp`, `strptime`, `regexp_extract`, `split_part`)
3. Casts types safely (`TRY_CAST`, `NULLIF`) if needed
4. Uses replace or append semantics based on Operation Mode
    - REPLACE:
      ```sql
      CREATE OR REPLACE TABLE {table_name} AS
        SELECT ... FROM read_xxx(...);
      ```
    - APPEND:
      ```sql
      CREATE TABLE IF NOT EXISTS {table_name} (...);
      INSERT INTO {table_name}
        SELECT ... FROM read_xxx(...);
      ```

## Examples

csv:

```sql
CREATE
OR REPLACE TABLE log_entries AS
SELECT to_timestamp(timestamp) ::TIMESTAMP AS timestamp,
    user_id,
    tenant_id,
    qualified_name
FROM read_csv('C:/logs/log.csv', header= true);
```

```sql
CREATE TABLE IF NOT EXISTS log_entries
(
    timestamp
    TIMESTAMP,
    user_id
    BIGINT,
    tenant_id
    BIGINT,
    method_fqn
    VARCHAR
);

INSERT INTO log_entries
SELECT strptime(timestamp, '%b %d, %Y @ %H:%M:%S.%f') ::TIMESTAMP AS timestamp,
    "cmap_user-id" AS user_id,
    "cmap_developer-id" AS tenant_id,
    "cmap_message" AS method_fqn
FROM read_csv('C:/log/sample.csv', header= true, timestampformat=' ');
```

json:

```sql
CREATE
OR REPLACE TABLE error_log AS
SELECT json_extract_string(data, '$.timestamp') AS timestamp,
    json_extract_string(data, '$.level') AS level,
    json_extract_string(data, '$.service') AS service,
    json_extract_string(data, '$.env') AS environment,
    json_extract_string(data, '$.error.code') AS error_code,
    json_extract_string(data, '$.error.message') AS message,
    json_extract_string(data, '$.error.file') AS file,
    json_extract_string(data, '$.error.line') AS line,
    json_extract_string(data, '$.error.stack') AS stack_trace,
    json_extract_string(data, '$.context.user_id') AS user_id,
    json_extract_string(data, '$.context.session_id') AS session_id,
    json_extract_string(data, '$.context.ip') AS ip_address
FROM read_json('C:/logs/error_log.json') AS data;

```

semi-structured text:

```sql
CREATE
OR REPLACE TABLE apache_log AS
SELECT split_part(t.line, ' ', 1) AS ip,
       strptime(split_part(split_part(t.line, '[', 2), ']', 1), '%d/%b/%Y:%H:%M:%S %z') ::TIMESTAMPTZ AS timestamp,
    split_part(split_part(t.line, '"', 2), ' ', 1) AS method,
    split_part(split_part(t.line, '"', 2), ' ', 2) AS path,
    split_part(split_part(t.line, '"', 2), ' ', 3) AS protocol,
    (regexp_extract(t.line, '" ([0-9]{3}) ', 1))::INTEGER AS status,
    NULLIF(regexp_extract(t.line, ' ([0-9]+) "[^"]*" "[^"]*"$', 1), '')::BIGINT AS bytes,
    regexp_extract(t.line, ' [0-9]+ "([^"]*)" ', 1) AS referer,
    regexp_extract(t.line, '"([^"]*)"$', 1) AS user_agent
FROM read_csv('C:/logs/apache_log.txt', auto_detect= false, timestampformat=' ', header= false, columns={'line':'VARCHAR'}, delim='\n', strict_mode= false
    ) AS t
WHERE t.line <> '';
```

```sql
CREATE
OR REPLACE TABLE new_table_1 AS
SELECT strptime(regexp_extract(t.line, '\[(.*?)\]', 1), '%a %b %d %H:%M:%S %Y') ::TIMESTAMP AS timestamp,
    regexp_extract(t.line, '\] \[(.*?)\]', 1) AS level,
    regexp_extract(t.line, '\] \[.*?\] ([^\[]+)$', 1) AS message
FROM read_csv('C:/logs/apache_log.log', delim='\n', header= false, columns={'line':'VARCHAR'}, strict_mode= false
    ) AS t
WHERE t.line <> '';

```

**Data Source Path:** `{{filePath}}`  
**Table Name:** `{{tableName}}`  
**Operation Mode:** `{{importMode}}`  
**Custom Description from User (optional):** {{additionalInfo}}

## CSV Sniffed Properties acquired using DuckDB sniff_csv (if input file is csv)

- NOTE: Sniffed properties are *only* used for context (e.g., column types) and should not be included in the SQL
  query, as DuckDB automatically applies them as defaults.

```sql
{{csvFileInfo}}
````

## Sample of Existing Table (if it exists):

```text
{{existingTableSample}}
```

## Sample Of Input File (5–10 lines):

```text
{{fileSample}}
```

## Additional Knowledge base:

1. Column names should contain only alphanumeric characters and underscores. Rename columns as needed to match this
   style unless explicitly instructed otherwise. Use quotes when dealing with such names in the created SQL query.
2. When parsing dates or timestamps, prefer: `to_timestamp(<unix_epoch_number>)::TIMESTAMP` or  
   `strptime(<string>, format) ::TIMESTAMP`, always add `timestampformat=' '` to avoid double parsing.
3. When dealing with raw or unknown formats, assume log-style input and use
   `read_csv(path, delim='\n', header=false, columns={'line':'VARCHAR'}, strict_mode=false)`
4. VERY IMPORTANT: Use raw regex patterns with single backslashes (e.g., \[, not \\[), since DuckDB SQL strings are
   single-quoted and do not require double escaping. Write regex exactly as you'd write it inside single quotes in SQL.

### Timestamp Format Specifiers (DuckDB)

| Specifier | Meaning                      | Example  |
|-----------|------------------------------|----------|
| `%Y`      | Full year                    | `2023`   |
| `%m`      | Month (01–12)                | `07`     |
| `%d`      | Day of month (01–31)         | `24`     |
| `%H`      | Hour (00–23)                 | `15`     |
| `%M`      | Minute (00–59)               | `03`     |
| `%S`      | Second (00–59)               | `09`     |
| `%f`      | Microseconds (000000–999999) | `123456` |
| `%z`      | Timezone offset              | `+0200`  |
| `%b`      | Abbreviated month            | `Jan`    |
| `%a`      | Abbreviated weekday          | `Mon`    |
| `%Z`      | Time zone name               | `UTC`    |

Use these with `strptime(...)` to correctly parse timestamps.

You may combine specifiers to match patterns like:

- `'%Y-%m-%d %H:%M:%S'` → `2023-07-24 15:03:09`
- `'%d/%b/%Y:%H:%M:%S %z'` → `24/Jul/2023:15:03:09 +0200` (Apache logs)

[Full DuckDB Timestamp Format Reference](https://duckdb.org/docs/sql/functions/strftime.html)
