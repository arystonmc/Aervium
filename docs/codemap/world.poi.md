# Aervium Code Map: `com.aryston.aervium.world.poi`

Part of the [Aervium code map](../CODEMAP.md). Every class of this package has its entry here and nowhere else (rule D1).

### `com.aryston.aervium.world.poi`

#### PoiColumns, PoiAudit
- Path: `src/main/java/com/aryston/aervium/world/poi/PoiColumns.java`, `src/main/java/com/aryston/aervium/world/poi/PoiAudit.java`
- Role: `PoiColumns` builds the column index for a level height (section key to chunk column and section index) and streams the records of a column's occupied sections from the lowest up, or returns null when the column is not fully loaded. `PoiAudit` (`-Daervium.poiAudit=true`) counts searches, records and searches whose indexed and vanilla records differ.
- Depends on: `ColumnIndexedLongMap`.
