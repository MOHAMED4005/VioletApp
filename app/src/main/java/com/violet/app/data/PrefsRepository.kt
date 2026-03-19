package com.violet.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "violet_prefs")

class PrefsRepository(private val context: Context) {

    companion object {
        // Vertimerge
        val VM_INPUT_URI    = stringPreferencesKey("vm_input_uri")
        val VM_OUTPUT_URI   = stringPreferencesKey("vm_output_uri")
        val VM_MERGE_MODE   = stringPreferencesKey("vm_merge_mode")   // "pixels" | "pages"
        val VM_PIXEL_HEIGHT = intPreferencesKey("vm_pixel_height")
        val VM_PAGE_COUNT   = intPreferencesKey("vm_page_count")
        val VM_SAVE_AS_ZIP  = booleanPreferencesKey("vm_save_as_zip")
        val VM_FORMAT       = stringPreferencesKey("vm_format")       // "JPG" | "PNG" | "WEBP"

        // Zip Maker
        val ZIP_INPUT_URI   = stringPreferencesKey("zip_input_uri")
        val ZIP_OUTPUT_URI  = stringPreferencesKey("zip_output_uri")

        // Watermark
        val WM_START_URI    = stringPreferencesKey("wm_start_uri")
        val WM_END_URI      = stringPreferencesKey("wm_end_uri")
        val WM_LOGO_URI     = stringPreferencesKey("wm_logo_uri")
        val WM_CHAPTER_URI  = stringPreferencesKey("wm_chapter_uri")
        val WM_OUTPUT_URI   = stringPreferencesKey("wm_output_uri")

        // Photo Format Changer
        val PFC_INPUT_URI   = stringPreferencesKey("pfc_input_uri")
        val PFC_OUTPUT_URI  = stringPreferencesKey("pfc_output_uri")
        val PFC_FORMAT      = stringPreferencesKey("pfc_format")
    }

    val prefs: Flow<Preferences> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    fun <T> get(key: Preferences.Key<T>, default: T): Flow<T> =
        prefs.map { it[key] ?: default }
}
