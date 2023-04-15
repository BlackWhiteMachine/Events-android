package com.positronen.events.data.converter.activities

import com.positronen.events.data.model.PlaceV2Response
import com.positronen.events.domain.model.PointDetailModel
import com.positronen.events.domain.model.PointModel

fun convertResponseActivities(activities: List<PlaceV2Response>): List<PointModel> = emptyList()

fun convertResponseActivitiesDetail(activities: PlaceV2Response): PointDetailModel? = null