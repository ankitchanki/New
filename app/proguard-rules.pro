-keep class com.vibe.stashdrop.core.shizuku.ShellUserService { *; }
-keep class com.vibe.stashdrop.core.shizuku.IPrivilegedService** { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }
# Keep line numbers so crash reports copied from the app map back to source with the release mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
