package com.devstudioworks.videoplayer.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

//@Singleton
class VideoPreferences(private val context: Context) {

    companion object {
        val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "video")
        private val VIDEO_URI = stringPreferencesKey("video_uri")
    }

    val getVideoUri: Flow<String?>
        get() = context.dataStore.data.map {
            it[VIDEO_URI]
        }

    suspend fun setVideoUri(value: String) {
        context.dataStore.edit {
            it[VIDEO_URI] = value
        }
    }

}