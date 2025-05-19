<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Changelog for Production Code Metrics Visualization

## [Unreleased]

### Changed
- Skip anonymous classes when resolving method FQN
- Decoupled context listener from ChartService scheduling logic

### Added
- Apache 2.0 license headers to source and build files
- Line Markers no longer appear on anonymous methods

### Fixed
- Removed `DumbAware` from PanelToolWindowFactory to avoid index access errors

### Docs
- Expanded README with use cases and updated Table of Contents

## [1.0.1] - 2025/05/18

### Fixed
- Made SQL editor area in config dialogs resizable.

### Changed
- Updated plugin name to "Production Code Metrics Visualization".

### Docs
- Updated README badges with the correct JetBrains Marketplace plugin ID.

## [1.0.0] - 2025/05/18

### Added

- **Embedded DuckDB Integration**
    - Local DuckDB database for project-specific metrics storage.
    - Configurable **Data Sources** to import external data (CSV, Parquet, JSON) via user-defined SQL.
    - Run imports, drop tables, and manage configurations via the Data Source settings.

- **Line Markers**
    - Inline metric markers next to Java methods, feature flag evaluations, or Spring mappings.
    - SQL-based value evaluation with customizable color rules (`from;to;color` format).
    - Supports context-aware SQL placeholders: `#method_fqn#`, `#feature_name#`, `#mapping_path#`, `#mapping_method#`,
      and file-wide variants.

- **Charts**
    - **Chart** tab in the "Code Metrics Visualizations" tool window.
    - Visualize metrics using SQL-driven charts (time series or categorical).
    - Dynamic filters for method, feature, path, and HTTP method (applied via context-aware SQL placeholders).
    - Supports context-aware filtering based on editor caret position, with an option to lock filters to fixed values.
    - Uses XChart for rendering.

- **LLM Prompt Generation**
    - Available for Data Sources, Line Markers, and Charts.
    - Generates AI-ready prompts containing natural language descriptions, table and file samples, and specific
      instructions to assist with SQL generation and configuration.

- **DB Viewer**
    - **DB Viewer** tab to browse imported tables and run custom `SELECT` queries.
    - Displays schema (columns, types) and total table row count.
    - Uses a read-only connection to prevent accidental data modification.

- **Feature Flag Support**
    - Configurable **Feature Evaluators** to identify feature flag calls in code.
    - Enables use of #feature_name# placeholder in context-aware queries.

- **Spring MVC Support**
    - Resolves mapping paths and HTTP methods from Spring annotations (e.g., `@GetMapping`).
    - Supports Spring-specific placeholders for charts and filtering integration.

- **Configuration UI**
    - Centralized settings under `Tools -> Production Code Metrics Visualization`.
    - Dedicated panels for Data Sources, Line Markers, Charts, and Feature Evaluators.
    - Supports adding, editing, and deleting configurations with validation and helper actions (e.g., LLM prompt
      generation buttons).

- **Other**
    - Plugin-specific notifications.
    - Tool window icon and group.
    - Basic error handling for SQL and config validation.
