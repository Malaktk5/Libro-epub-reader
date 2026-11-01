package app.libro.reader

import android.app.Application
import app.libro.reader.data.LibraryRepository
import app.libro.reader.data.Prefs

class LibroApp : Application() {
    lateinit var prefs: Prefs
        private set
    lateinit var library: LibraryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        library = LibraryRepository(this)
    }
}
