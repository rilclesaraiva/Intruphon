package com.intruphon.app.data.download

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.MediaStore
import com.intruphon.app.data.settings.AppSettings
import java.io.IOException

/**
 * Where saved files go. By default the app writes through MediaStore into Download/Intruphon. When the person
 * picks a folder with Android's folder picker, the app keeps that folder's permission and writes into it
 * (no storage permission is involved), with the same Audio and Video subfolders.
 */
object SaveLocation {
    private val lock = Any()

    /** The folder the person picked, as long as its permission is still held; otherwise null (the default folder). Reads only. */
    fun picked(context: Context): Uri? {
        val saved = AppSettings.downloadFolder(context) ?: return null
        val uri = Uri.parse(saved)
        val held = context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission && it.isWritePermission }
        return if (held) uri else null
    }

    /** True when a folder was picked but Android no longer lets the app use it: downloads then go to the default folder. */
    fun lost(context: Context): Boolean = AppSettings.downloadFolder(context) != null && picked(context) == null

    /**
     * Keeps the permission for [tree] and makes it the download folder. Permissions of folders used before are kept
     * on purpose: files saved there must stay reachable (open, delete) from the Downloads list. Throws if Android refuses.
     */
    fun choose(context: Context, tree: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(tree, flags)   // first: if it fails, the old choice stays
        AppSettings.setDownloadFolder(context, tree.toString())
    }

    /** Goes back to Download/Intruphon. Files already saved stay where they are and stay reachable. */
    fun useDefault(context: Context) {
        AppSettings.setDownloadFolder(context, null)
    }

    /** The picked folder as a path to show, for example /storage/emulated/0/Music/Intruphon; null for the default folder. */
    fun label(context: Context): String? = picked(context)?.let { pathOf(DocumentsContract.getTreeDocumentId(it)) }

    // A document id looks like "primary:Music/Intruphon" (volume, then the path inside it).
    private fun pathOf(documentId: String): String {
        val volume = documentId.substringBefore(':')
        val path = documentId.substringAfter(':', "")
        val root = if (volume == "primary") "/storage/emulated/0" else "/storage/$volume"
        return if (path.isEmpty()) root else "$root/$path"
    }

    /** Creates an empty file named [name] in [subfolder] of the picked [tree] (the subfolder is created when missing). */
    fun create(context: Context, tree: Uri, subfolder: String, name: String, mime: String): Uri {
        val resolver = context.contentResolver
        val dir = synchronized(lock) {   // several downloads start together: make the subfolder only once
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            childFolder(resolver, tree, root, subfolder)
                ?: DocumentsContract.createDocument(resolver, root, Document.MIME_TYPE_DIR, subfolder)
                ?: throw IOException("Could not create the $subfolder folder")
        }
        return DocumentsContract.createDocument(resolver, dir, mime, name) ?: throw IOException("Could not create the file")
    }

    private fun childFolder(resolver: ContentResolver, tree: Uri, parent: Uri, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent))
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
        resolver.query(children, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == name && cursor.getString(2) == Document.MIME_TYPE_DIR) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                }
            }
        }
        return null
    }

    /** Deletes a saved file, whether it lives in MediaStore (default folder) or in the picked folder. */
    fun delete(resolver: ContentResolver, uri: Uri) {
        if (uri.authority == MediaStore.AUTHORITY) resolver.delete(uri, null, null)
        else DocumentsContract.deleteDocument(resolver, uri)
    }

    /** Asks the media index to look at a file saved in the picked folder, so music and gallery apps see it. Best effort. */
    fun index(context: Context, uri: Uri, mime: String) {
        runCatching {
            val path = pathOf(DocumentsContract.getDocumentId(uri))
            MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mime), null)
        }
    }

    // Files being written into the picked folder: if the process dies, they are removed the next time the app starts.
    private const val PARTIALS = "partials"
    private val partialLock = Any()

    private fun partialPrefs(context: Context) = context.getSharedPreferences(PARTIALS, Context.MODE_PRIVATE)

    fun trackPartial(context: Context, uri: Uri) = synchronized(partialLock) {
        val prefs = partialPrefs(context)
        prefs.edit().putStringSet(PARTIALS, (prefs.getStringSet(PARTIALS, emptySet()).orEmpty() + uri.toString())).apply()
    }

    fun untrackPartial(context: Context, uri: Uri) = synchronized(partialLock) {
        val prefs = partialPrefs(context)
        prefs.edit().putStringSet(PARTIALS, (prefs.getStringSet(PARTIALS, emptySet()).orEmpty() - uri.toString())).apply()
    }

    /** Deletes what a dead process left half-written in the picked folder. Call off the main thread, before any download starts. */
    fun cleanPartials(context: Context) {
        val left = synchronized(partialLock) {
            val prefs = partialPrefs(context)
            prefs.getStringSet(PARTIALS, emptySet()).orEmpty().also { prefs.edit().remove(PARTIALS).apply() }
        }
        left.forEach { runCatching { delete(context.contentResolver, Uri.parse(it)) } }
    }
}
