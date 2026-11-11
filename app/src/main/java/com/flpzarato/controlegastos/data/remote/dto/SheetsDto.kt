package com.flpzarato.controlegastos.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ValueRange(
    val range: String? = null,
    val majorDimension: String = "ROWS",
    val values: List<List<String>> = emptyList(),
)

@Serializable
data class BatchGetValuesResponse(
    val spreadsheetId: String? = null,
    val valueRanges: List<ValueRange> = emptyList(),
)

@Serializable
data class UpdateValuesResponse(
    val updatedRange: String? = null,
    val updatedRows: Int? = null,
)

@Serializable
data class AppendValuesResponse(
    val spreadsheetId: String? = null,
    val tableRange: String? = null,
    val updates: UpdateValuesResponse? = null,
)

@Serializable
data class BatchUpdateValuesRequest(
    val valueInputOption: String = "RAW",
    val data: List<ValueRange>,
)

@Serializable
data class BatchUpdateValuesResponse(
    val spreadsheetId: String? = null,
    val totalUpdatedRows: Int? = null,
    val totalUpdatedColumns: Int? = null,
    val totalUpdatedCells: Int? = null,
    val totalUpdatedSheets: Int? = null,
    val responses: List<UpdateValuesResponse> = emptyList(),
)
