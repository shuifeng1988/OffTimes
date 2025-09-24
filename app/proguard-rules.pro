# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# 保留行号信息用于崩溃分析和Google Play错误报告
-keepattributes SourceFile,LineNumberTable

# 保留源文件名用于调试
-renamesourcefileattribute SourceFile

# 保留异常信息用于问题排查
-keepattributes Exceptions

# 保留注解信息
-keepattributes *Annotation*

# 保留泛型信息
-keepattributes Signature

# Hilt: Keep AppInstallReceiver to prevent conflicts with bytecode instrumentation
-keep class com.offtime.app.receiver.AppInstallReceiver { *; }

# Hilt: Keep ScreenStateReceiver to prevent conflicts with bytecode instrumentation
-keep class com.offtime.app.receiver.ScreenStateReceiver { *; }

# ========== Google Play 警告修复 ==========

# 保留关键服务类防止崩溃
-keep class com.offtime.app.service.** { *; }

# 保留Widget相关类
-keep class com.offtime.app.widget.** { *; }

# 保留数据库实体类
-keep class com.offtime.app.data.entity.** { *; }

# 保留支付相关类
-keep class com.offtime.app.manager.**PaymentManager { *; }
-keep class com.offtime.app.manager.GooglePlayBillingManager { *; }

# 保留关键工具类的日志方法
-keep class com.offtime.app.utils.** {
    public static void d(...);
    public static void i(...);
    public static void w(...);
    public static void e(...);
}

# 保留应用生命周期相关类
-keep class com.offtime.app.util.AppLifecycleObserver { *; }

# 保留崩溃报告相关的方法
-keepclassmembers class * {
    public void onCrash(...);
    public void reportCrash(...);
    public void logError(...);
}

# 保留反射调用的方法（支付相关）
-keepclassmembers class * {
    public void initialize();
    public void startConnection();
    public void onBillingSetupFinished(...);
}

# 防止混淆导致的运行时异常
-keep class kotlin.reflect.** { *; }
-keep class kotlin.coroutines.** { *; }

# Retrofit和网络相关保留规则
-keep class retrofit2.** { *; }
-keep class com.offtime.app.data.network.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# 保留Retrofit接口方法
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# 保留Gson相关类
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 保留网络请求和响应数据类
-keep class com.offtime.app.data.network.ApiResponse { *; }
-keep class com.offtime.app.data.network.*Request { *; }
-keep class com.offtime.app.data.network.*Response { *; }

# 保留Compose相关类（优化内存使用）
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }

# Room数据库保留规则
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Hilt/Dagger保留规则
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# ========== 调试信息保留 ==========

# 保留所有日志相关的方法调用（用于生产环境问题排查）
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
} 

# 但保留错误和警告日志
-keep class android.util.Log {
    public static int w(...);
    public static int e(...);
} 