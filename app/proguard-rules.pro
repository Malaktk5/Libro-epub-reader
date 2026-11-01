# Methods called from JavaScript through addJavascriptInterface must keep their names.
-keepclassmembers class app.libro.reader.ui.reader.ReaderController$Bridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
