package com.positronen.events.data.service

import com.positronen.events.data.model.DataResponse
import com.positronen.events.data.model.EventsV1Response
import com.positronen.events.data.model.PlaceV2Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MainService {

    @GET("/v2/places/")
    suspend fun places(
        @Query(value = "distance_filter") distanceFilter: String?,
        @Query(value = "start") start: Int,
        @Query(value = "limit") limit: Int
    ): DataResponse<List<PlaceV2Response>>

    @GET("/v2/place/{id}")
    suspend fun place(
        @Path("id") id: String
    ): PlaceV2Response

    @GET("/v1/events/")
    suspend fun events(
        @Query(value = "distance_filter") distanceFilter: String?,
        @Query(value = "start") start: Int,
        @Query(value = "limit") limit: Int
    ): DataResponse<List<EventsV1Response>>

    @GET("/v1/event/{id}")
    suspend fun event(
        @Path("id") id: String
    ): EventsV1Response

    @GET("/v2/activities/")
    suspend fun activities(
        @Query(value = "start") start: Int,
        @Query(value = "limit") limit: Int
    ): DataResponse<List<PlaceV2Response>>

    @GET("/v2/activity/{id}")
    suspend fun activity(
        @Path("id") id: String
    ): PlaceV2Response

}