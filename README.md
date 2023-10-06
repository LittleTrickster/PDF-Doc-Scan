# PDF-Doc-Scan
Android PDF document scanning app

Main point of this app is to scan or select existing PDF documents simply using intent or chooser.
Other apps require payment to access their api/sdk or simply don't have a possible way to do it. So they rely on switching between an app ,file chooser and a scanner.

![Alt text](/misc/vid.gif?raw=true  "Demo video")

[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"
     alt="Get it on IzzyOnDroid"
     height="80">](https://apt.izzysoft.de/fdroid/index/apk/com.littletrickster.scanner)

or get the APK from the [Releases section](https://github.com/LittleTrickster/PDF-Doc-Scan/releases/latest).

## Usage Example

Create ActivityResultLauncher
```kotlin
val pdfResultLauncher = registerForActivityResult(StartActivityForResult()) {
    val uri = it.data?.data
    if (it.resultCode == Activity.RESULT_OK && uri != null) {
//        context.contentResolver.openInputStream(uri)
    }
}
```
Or using Compose
```kotlin
val pdfResultLauncher = rememberLauncherForActivityResult(contract = StartActivityForResult()){
    val uri = it.data?.data
    if (it.resultCode == Activity.RESULT_OK && uri != null) {
//        context.contentResolver.openInputStream(uri)
    }
}
```

If you target Android 11 (API level 30) and up add query to your AndroidManifest.xml to make it visible for your app.
```xml
<queries>
<package android:name="com.littletrickster.scanner" />
</queries>
```

Query and launch directly
```kotlin
val intent = Intent()
intent.component = ComponentName("com.littletrickster.scanner", "com.littletrickster.scanner.ScanActivity")
intent.action = Intent.ACTION_PICK
intent.type = "application/pdf"

val pdfInfo = context.packageManager.queryIntentActivities(intent, 0)
if (pdfInfo.firstOrNull()?.activityInfo?.enabled == true) {
    pdfResultLauncher.launch(intent)
}
```

### Comments
Made this as a prototype while learning Compose framework in late 2021
