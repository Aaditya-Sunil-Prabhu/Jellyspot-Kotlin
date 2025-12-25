package com.jellyspot.data.repository

import com.jellyspot.data.local.entities.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for YouTube Music operations using NewPipe Extractor.
 */
@Singleton
class YouTubeRepository @Inject constructor() {

    private val youtubeService: YoutubeService by lazy {
        ServiceList.YouTube as YoutubeService
    }

    init {
        // Initialize NewPipe with default downloader
        try {
            NewPipe.init(object : org.schabi.newpipe.extractor.downloader.Downloader() {
                override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): org.schabi.newpipe.extractor.downloader.Response {
                    val url = java.net.URL(request.url())
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    
                    connection.requestMethod = request.httpMethod()
                    connection.connectTimeout = 30000
                    connection.readTimeout = 30000
                    
                    request.headers().forEach { (key, values) ->
                        values.forEach { value ->
                            connection.addRequestProperty(key, value)
                        }
                    }
                    
                    request.dataToSend()?.let { data ->
                        connection.doOutput = true
                        connection.outputStream.write(data)
                    }
                    
                    val responseCode = connection.responseCode
                    val responseBody = try {
                        connection.inputStream.bufferedReader().readText()
                    } catch (e: Exception) {
                        connection.errorStream?.bufferedReader()?.readText() ?: ""
                    }
                    
                    val responseHeaders = connection.headerFields
                        .filter { it.key != null }
                        .mapValues { it.value }
                    
                    return org.schabi.newpipe.extractor.downloader.Response(
                        responseCode,
                        "",
                        responseHeaders,
                        responseBody,
                        null
                    )
                }
            })
        } catch (e: Exception) {
            // Already initialized
        }
    }

    /**
     * Search for music on YouTube Music.
     */
    suspend fun search(query: String, limit: Int = 30): List<TrackEntity> = withContext(Dispatchers.IO) {
        try {
            val searchUrl = youtubeService.searchQHFactory
                .fromQuery(query, listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS), "")
                .url
            
            val extractor = youtubeService.getSearchExtractor(searchUrl)
            extractor.fetchPage()
            
            extractor.initialPage.items
                .filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                .take(limit)
                .map { item ->
                    TrackEntity(
                        id = "youtube_${item.url.hashCode()}",
                        name = item.name,
                        artist = item.uploaderName ?: "Unknown Artist",
                        album = "YouTube Music",
                        durationMillis = item.duration * 1000,
                        streamUrl = item.url,
                        imageUrl = item.thumbnails.maxByOrNull { it.height }?.url,
                        source = "youtube"
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get stream URL for playback.
     */
    suspend fun getStreamUrl(videoUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val streamInfo = StreamInfo.getInfo(youtubeService, videoUrl)
            
            // Prefer audio-only streams
            val audioStream = streamInfo.audioStreams
                .filter { it.isAudioOnly }
                .maxByOrNull { it.averageBitrate }
            
            if (audioStream != null) {
                return@withContext audioStream.content
            }
            
            // Fall back to video stream with best audio
            val videoStream = streamInfo.videoOnlyStreams
                .maxByOrNull { it.bitrate }
            
            videoStream?.content
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get trending music.
     */
    suspend fun getTrendingMusic(limit: Int = 20): List<TrackEntity> = withContext(Dispatchers.IO) {
        try {
            // Use trending/charts page
            val kioskList = youtubeService.kioskList
            val trendingExtractor = kioskList.getExtractorById("Trending", null)
            trendingExtractor.fetchPage()
            
            trendingExtractor.initialPage.items
                .filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                .filter { item ->
                    // Filter for music content
                    item.duration > 60 && item.duration < 600 // 1-10 minutes
                }
                .take(limit)
                .map { item ->
                    TrackEntity(
                        id = "youtube_${item.url.hashCode()}",
                        name = item.name,
                        artist = item.uploaderName ?: "Unknown",
                        album = "YouTube Music",
                        durationMillis = item.duration * 1000,
                        streamUrl = item.url,
                        imageUrl = item.thumbnails.maxByOrNull { it.height }?.url,
                        source = "youtube"
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get track details.
     */
    suspend fun getTrackDetails(videoUrl: String): TrackEntity? = withContext(Dispatchers.IO) {
        try {
            val streamInfo = StreamInfo.getInfo(youtubeService, videoUrl)
            
            TrackEntity(
                id = "youtube_${videoUrl.hashCode()}",
                name = streamInfo.name,
                artist = streamInfo.uploaderName ?: "Unknown",
                album = "YouTube Music",
                durationMillis = streamInfo.duration * 1000,
                streamUrl = videoUrl,
                imageUrl = streamInfo.thumbnails.maxByOrNull { it.height }?.url,
                source = "youtube"
            )
        } catch (e: Exception) {
            null
        }
    }
}
