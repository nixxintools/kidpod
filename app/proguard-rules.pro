# Keep Room entities
-keep class com.kidpod.app.data.database.entities.** { *; }

# Keep ExoPlayer
-keep class androidx.media3.** { *; }

# Keep Device Admin receiver
-keep class com.kidpod.app.receiver.DeviceAdminReceiver { *; }

# Keep parcelable classes
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
