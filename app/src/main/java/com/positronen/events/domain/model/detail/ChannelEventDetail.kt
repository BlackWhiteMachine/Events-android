package com.positronen.events.domain.model.detail

sealed class ChannelEventDetail{
    data class ShareText(val text: String): ChannelEventDetail()
}
