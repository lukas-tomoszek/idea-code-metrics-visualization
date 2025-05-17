# Generate DuckDB SQL for Chart

**Task:**
Generate a DuckDB SQL query template suitable for creating a chart.
The SQL query must:

1. Return **two columns**:
    - The first column will be used for the X-axis (labels, categories, or time series).
    - The second column will be used for the Y-axis (values).

2. Use placeholders (if relevant):
    - `#method_fqn#`: Fully qualified name of the user-selected method from the current file (e.g.,
      `com.example.Foo.getUser`, or `%` if "All Methods" selected).
    - `#feature_name#`: Name of the user-selected feature from the current file (e.g., `new_checkout_enabled`, or `%`
      if "All
      Features" selected).
    - `#method_fqns_in_file#`: A CSV of all method FQNs in the file (e.g.,
      `'com.example.Foo.get','com.example.Bar.post'`).
    - `#feature_names_in_file#`: A CSV of all feature flags in the file (e.g., `'feat_a','feat_b'`).
    - `#mapping_path#`: Regex for selected mapping (e.g., `/users/[^/]+`, or `.*` if "All Paths" selected).
    - `#mapping_method#`: HTTP method string (e.g., `GET`, or `%` if "All HTTP Methods" selected).
    - `#mapping_paths_in_file#`: Regex alternation of mappings in the file (e.g., `/users/[^/]+|/orders/[^/]+/confirm`).
    - `#mapping_methods_in_file#`: CSV of all HTTP methods in the file (e.g., `'GET','POST'`).

**Examples:**

```sql
-- Errors over time
SELECT date_trunc('day', timestamp) AS day, COUNT(*) AS errors
FROM my_logs
WHERE event = 'ERROR'
GROUP BY day
ORDER BY day;

-- Feature Usage Ratio Among Users of Selected Method
SELECT uf.feature_name                                                 AS feature,
       COUNT(DISTINCT uf.user_id) * 100.0 / COUNT(DISTINCT mu.user_id) AS usage_ratio
FROM user_feature uf
         JOIN log_entries mu ON uf.user_id = mu.user_id
WHERE mu.method_name LIKE '#method_fqn#'
  AND uf.feature_name IN (#feature_names_in_file#)
GROUP BY uf.feature_name
ORDER BY uf.feature_name;


-- Call counts for methods in the file
SELECT method_name, COUNT(*) AS call_count
FROM log_entries
WHERE method_name IN (#method_fqns_in_file#)
GROUP BY method_name
ORDER BY call_count DESC;

-- Request volume per day for matching mapping
SELECT date_trunc('day', timestamp) AS day,
  COUNT(*) AS request_count
FROM apache_log
WHERE method LIKE '#mapping_method#'
  AND REGEXP_MATCHES(path
    , '^#mapping_path#/?(\?.*)?$')
  AND REGEXP_MATCHES(path
    , '^(#mapping_paths_in_file#)(/.*)?(\?.*)?$')
GROUP BY day
ORDER BY day;
```

3. Refer to the table samples below for available columns and data structure.

**Custom Task Description from User (optional):**
{{llmDescription}}

**Available Table Samples (Top 10 rows using `USING SAMPLE 10 ROWS`):**
{{tableSamples}}
