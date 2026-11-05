package com.flpzarato.controlegastos.data.remote

import com.flpzarato.controlegastos.data.remote.dto.AppendValuesResponse
import com.flpzarato.controlegastos.data.remote.dto.BatchGetValuesResponse
import com.flpzarato.controlegastos.data.remote.dto.ValueRange
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Thin wrapper over the Google Sheets API v4 REST endpoints this app needs. */
interface SheetsApi {

    @GET("v4/spreadsheets/{spreadsheetId}/values:batchGet")
    suspend fun batchGet(
        @Path("spreadsheetId") spreadsheetId: String,
        @Query("ranges") ranges: List<String>,
    ): BatchGetValuesResponse

    @PUT("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun update(
        @Path("spreadsheetId") spreadsheetId: String,
        @Path(value = "range", encoded = true) range: String,
        @Query("valueInputOption") valueInputOption: String = "RAW",
        @Body body: ValueRange,
    ): ValueRange

    @POST("v4/spreadsheets/{spreadsheetId}/values/{range}:append")
    suspend fun append(
        @Path("spreadsheetId") spreadsheetId: String,
        @Path(value = "range", encoded = true) range: String,
        @Query("valueInputOption") valueInputOption: String = "RAW",
        @Query("insertDataOption") insertDataOption: String = "INSERT_ROWS",
        @Body body: ValueRange,
    ): AppendValuesResponse

    companion object {
        const val BASE_URL = "https://sheets.googleapis.com/"
    }
}
