# Generate DuckDB SQL and Coloring Rules for Line Marker

**Task 1: Generate a DuckDB SQL query template for Line Marker**
The SQL query must:

1. Return a **single numerical value**.

2. Use the following placeholders as needed:
    - `#method_fqn#`: Fully qualified name of the method at the marker (e.g., `com.example.Foo.getUser`)
    - `#feature_name#`: Feature flag near the marker (e.g., `new_checkout_enabled`)
    - `#method_fqns_in_file#`: CSV of all method FQNs (e.g., `'com.example.Foo.get','com.example.Bar.post'`)
    - `#feature_names_in_file#`: CSV of all feature names (e.g., `'feat_a','feat_b'`)
    - `#mapping_path#`: Regex of a selected HTTP path (e.g., `/users/[^/]+`)
    - `#mapping_method#`: HTTP method (e.g., `'GET'`)
    - `#mapping_paths_in_file#`: Regex alternation of all mappings (e.g., `/users/[^/]+|/orders/[^/]+/confirm`)
    - `#mapping_methods_in_file#`: CSV of methods (e.g., `'GET','POST'`)

**Examples for SQL:**

```sql
-- Errors in current method
SELECT COUNT(*)
FROM my_logs
WHERE method_fqn = '#method_fqn#'
  AND event = 'ERROR';

-- Latency for method + feature
SELECT AVG(latency_ms)
FROM performance_metrics
WHERE method_fqn = '#method_fqn#'
  AND feature_flag = '#feature_name#';

-- Proportional call count for methods in file
SELECT COUNT(*) FILTER (WHERE method_name = '#method_fqn#') * 1.0 /
  NULLIF(COUNT(*), 1)
FROM log_entries_100
WHERE method_name IN (#method_fqns_in_file#);

-- Requests for a specific mapping in current file
SELECT COUNT(*)
FROM apache_log
WHERE method = '#mapping_method#'
  AND REGEXP_MATCHES(path, '^#mapping_path#/?(\?.*)?$')
  AND REGEXP_MATCHES(path, '^(#mapping_paths_in_file#)(/.*)?(\?.*)?$');
```

**Task 2: Suggest Coloring Rules**
Based on the returned value from Task 1, suggest coloring rules.

- Format: `from_exclusive_or_empty;to_inclusive_or_empty;hex_color_or_empty`
- Empty values imply open-ended ranges or "no color"
- The first matching rule is applied

**Examples:**
Percentage metric:

```
;50;#FF0000
50;75;#FFFF00
75;;#00FF00
```

Error count metric:

```
;0;#00FF00
0;5;#FFFF00
5;;#FF0000
```

**Custom Task Description from User (optional):**
{{llmDescription}}

**Available Table Samples (Top 10 rows using `USING SAMPLE 10 ROWS`):**
{{tableSamples}}
