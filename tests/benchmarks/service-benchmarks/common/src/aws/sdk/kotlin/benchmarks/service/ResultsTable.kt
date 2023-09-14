/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service

import aws.sdk.kotlin.benchmarks.service.telemetry.MetricSummary
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

private const val NAME_FIELD = "name"
private const val COUNT_FIELD = "n"

/**
 * A set of results from service benchmarks. The results are keyed first by service, then by operation, then by metric
 * name. For instance:
 *
 * ```json
 * {
 *   "S3": {
 *     "HeadObject": {
 *       "Overhead (ms)": {
 *         "count": 1618,
 *         "statistics": {
 *           "min": 0.340,
 *           "avg": 0.605,
 *           "med": 0.417,
 *           ...
 *         }
 *       },
 *       ...
 *     },
 *     ...
 *   },
 *   ...
 * }
 * ```
 */
private typealias Results = Map<String, Map<String, Map<String, MetricSummary>>>

class ResultsTable private constructor(private val columns: List<Column>, private val rows: List<Array<String>>) {
    companion object {
        fun from(results: Results): ResultsTable {
            val columnMapper = ColumnMapper.from(results)
            val rows = RowGenerator(columnMapper).generate(results)

            val maxWidths = Array(columnMapper.count) { 0 }
            rows.forEach { row ->
                row.forEachIndexed { idx, value ->
                    maxWidths[idx] = max(maxWidths[idx], value.length)
                }
            }

            val columns = maxWidths.mapIndexed { index, maxWidth ->
                val alignment = if (index < 1) HorizontalAlignment.LEFT else HorizontalAlignment.RIGHT
                Column(maxWidth, alignment)
            }

            return ResultsTable(columns, rows)
        }
    }

    override fun toString(): String = buildString {
        rows.forEach { row ->
            append('|')
            row.forEachIndexed { index, cell ->
                val column = columns[index]
                append(' ')
                append(column.alignment.pad(cell, column.maxWidth))
                append(" |")
            }
            appendLine()
        }
    }
}

private enum class HorizontalAlignment(val pad: String.(Int) -> String) {
    LEFT(String::padEnd),
    RIGHT(String::padStart),
}

private data class Column(val maxWidth: Int, val alignment: HorizontalAlignment)

private data class ColumnMapper(val count: Int, val mapping: Map<String, Map<String, Int>>) {
    companion object {
        fun from(results: Results): ColumnMapper {
            var count = 1 // One for the left-most column holding subject

            val mapping = mutableMapOf<String, MutableMap<String, Int>>()
            results.values.forEach { service ->
                service.values.forEach { operation ->
                    operation.entries.forEach { (metric, summary) ->
                        val metricMapping = mapping.getOrPut(metric, ::mutableMapOf)
                        metricMapping.getOrPut(NAME_FIELD) { count++ }
                        metricMapping.getOrPut(COUNT_FIELD) { count++ }
                        summary.statistics.keys.forEach { statistic ->
                            metricMapping.getOrPut(statistic) { count++ }
                        }
                    }
                }
            }

            return ColumnMapper(count, mapping)
        }
    }
}

private data class RowGenerator(val columnMapper: ColumnMapper) {
    fun generate(results: Results) = buildList {
        add(headerRow())
        add(delineatorRow())

        // Value rows
        results.forEach { (service, operations) ->
            add(serviceRow(service))
            operations.forEach { (operation, metrics) ->
                add(operationRow(operation, metrics))
            }
        }
    }

    private fun delineatorRow() = row { idx -> if (idx < 1) ":---" else "---:" }

    private fun headerRow(): Array<String> {
        val header = row()
        columnMapper.mapping.forEach { (metric, metricColumnMapping) ->
            metricColumnMapping.keys.forEach { statistic ->
                header[metricColumnMapping.getValue(statistic)] = when (statistic) {
                    NAME_FIELD -> metric
                    else -> statistic
                }
            }
        }
        return header
    }

    private fun operationRow(operation: String, metrics: Map<String, MetricSummary>): Array<String> {
        val row = row()
        row[0] = "  —$operation"
        metrics.forEach { (metric, summary) ->
            val metricColumnMapping = columnMapper.mapping.getValue(metric)
            row[metricColumnMapping.getValue(COUNT_FIELD)] = summary.count.toString()
            summary.statistics.forEach { (statistic, value) ->
                row[metricColumnMapping.getValue(statistic)] = value.format()
            }
        }
        return row
    }

    private fun serviceRow(service: String) = row { idx -> if (idx == 0) "**$service**" else "" }

    private fun row(init: (Int) -> String = { "" }) = Array(columnMapper.count, init)
}

private fun Double.format(precision: Int = 3): String {
    val magnitude = 10.0.pow(precision)
    val default = (round(this * magnitude) / magnitude).toString()
    val chunks = default.split(".")
    return chunks[0] + "." + if (chunks.size == 1) {
        "0".repeat(precision)
    } else {
        chunks[1].padEnd(precision, '0')
    }
}
