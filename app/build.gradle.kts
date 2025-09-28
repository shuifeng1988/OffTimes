plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    // id("com.google.gms.google-services") // 移除以解决Firebase相关的前台服务问题
}

// Google Services插件已在plugins块中应用

android {
    namespace = "com.offtime.app"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35
        versionCode = 31
        versionName = "1.4.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // 支持16 KB页面大小 (Android 15+兼容性要求)
        ndk {
            // 启用16 KB页面对齐
            debugSymbolLevel = "SYMBOL_TABLE"
        }
    }

    // 产品维度配置 - 区分支付宝版本和Google Play版本
    flavorDimensions += "version"
    productFlavors {
        create("alipay") {
            dimension = "version"
            applicationId = "com.offtime.app"
            versionNameSuffix = "-alipay"
            
            // 支付宝配置
            buildConfigField("String", "ALIPAY_APP_ID", "\"${project.findProperty("ALIPAY_APP_ID") ?: ""}\"")
            buildConfigField("String", "ALIPAY_MERCHANT_PRIVATE_KEY", "\"${project.findProperty("ALIPAY_MERCHANT_PRIVATE_KEY") ?: ""}\"")
            buildConfigField("String", "ALIPAY_PUBLIC_KEY", "\"${project.findProperty("ALIPAY_PUBLIC_KEY") ?: ""}\"")
            buildConfigField("boolean", "ALIPAY_IS_SANDBOX", "${project.findProperty("ALIPAY_IS_SANDBOX") ?: "true"}")
            
            // 功能开关
            buildConfigField("boolean", "ENABLE_ALIPAY_LOGIN", "false")  // 删除支付宝登录
            buildConfigField("boolean", "ENABLE_SMS_LOGIN", "true")
            buildConfigField("boolean", "ENABLE_PASSWORD_LOGIN", "true")
            buildConfigField("boolean", "ENABLE_GOOGLE_LOGIN", "false")
            buildConfigField("boolean", "ENABLE_GOOGLE_PAY", "false")
            buildConfigField("boolean", "REQUIRE_SMS_VERIFICATION", "false")  // 支付宝版本不需要SMS验证码
        }
        
        create("googleplay") {
            dimension = "version"
            applicationId = "com.offtime.app.gplay"
            versionNameSuffix = "-gplay"
            
            // Google配置
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${project.findProperty("GOOGLE_WEB_CLIENT_ID") ?: ""}\"")
            buildConfigField("String", "GOOGLE_PLAY_LICENSE_KEY", "\"${project.findProperty("GOOGLE_PLAY_LICENSE_KEY") ?: ""}\"")
            
            // 功能开关
            buildConfigField("boolean", "ENABLE_ALIPAY_LOGIN", "false")
            buildConfigField("boolean", "ENABLE_SMS_LOGIN", "false")
            buildConfigField("boolean", "ENABLE_PASSWORD_LOGIN", "false")
            buildConfigField("boolean", "ENABLE_GOOGLE_LOGIN", "true")
            buildConfigField("boolean", "ENABLE_GOOGLE_PAY", "true")
            buildConfigField("boolean", "REQUIRE_SMS_VERIFICATION", "true")  // Google Play版本保持需要SMS验证码
            
            // 支付宝配置设为空（不使用）
            buildConfigField("String", "ALIPAY_APP_ID", "\"\"")
            buildConfigField("String", "ALIPAY_MERCHANT_PRIVATE_KEY", "\"\"")
            buildConfigField("String", "ALIPAY_PUBLIC_KEY", "\"\"")
            buildConfigField("boolean", "ALIPAY_IS_SANDBOX", "false")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("../app-release-key.jks")
            storePassword = "offtime2024"
            keyAlias = "offtime-key"
            keyPassword = "offtime2024"
        }
    }

    buildTypes {
        debug {
            // Debug版本禁用Google登录，避免配置问题
            buildConfigField("boolean", "ENABLE_GOOGLE_LOGIN", "false")
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            
            // 为Google Play Console生成调试符号
            ndk {
                debugSymbolLevel = "FULL"
            }
            
            // 确保生成mapping文件用于崩溃分析
            isDebuggable = false
            isJniDebuggable = false
            isRenderscriptDebuggable = false
            
            // 保留关键调试信息用于生产环境问题排查
            buildConfigField("boolean", "ENABLE_RELEASE_LOGGING", "true")
            
            // 启用崩溃报告
            manifestPlaceholders["crashlyticsCollectionEnabled"] = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // 支持16 KB页面大小对齐 (Android 15+兼容性)
        jniLibs {
            useLegacyPackaging = false
            // 排除有问题的AndroidX Graphics库，使用软件渲染替代
            excludes += "**/libandroidx.graphics.path.so"
        }
    }
    
    lint {
        baseline = file("lint-baseline.xml")
    }
    
    // Android App Bundle配置 - 支持16 KB页面大小
    bundle {
        language {
            // 启用语言资源拆分
            enableSplit = true
        }
        density {
            // 启用密度资源拆分
            enableSplit = true
        }
        abi {
            // 启用ABI拆分并确保16 KB对齐
            enableSplit = true
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    
    // Compose BOM - 更新到最新版本以支持16 KB对齐
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Firebase 完全移除以解决前台服务启动问题
    // implementation(platform("com.google.firebase:firebase-bom:32.6.0"))
    // implementation("com.google.firebase:firebase-analytics-ktx")
    
    
    
    // Accompanist for SwipeRefresh
    implementation("com.google.accompanist:accompanist-swiperefresh:0.32.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.1")
    
    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Retrofit & Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Alipay SDK - 只在支付宝版本中使用
    "alipayImplementation"("com.alipay.sdk:alipaysdk-android:15.8.35")
    
    // Google Play Services - 只在Google Play版本中使用
    "googleplayImplementation"("com.google.android.gms:play-services-auth:20.7.0")
    "googleplayImplementation"("com.google.android.gms:play-services-base:18.2.0")
    
    // Google Play Billing - 只在Google Play版本中使用 (升级到7.0.0以满足Google Play要求)
    "googleplayImplementation"("com.android.billingclient:billing-ktx:7.0.0")

    // Google Mobile Ads SDK (AdMob) - 只在Google Play版本中使用
    "googleplayImplementation"("com.google.android.gms:play-services-ads:23.1.0")
    
    // Firebase 完全移除以解决前台服务启动问题
    // "googleplayImplementation"(platform("com.google.firebase:firebase-bom:32.6.0"))
    // "googleplayImplementation"("com.google.firebase:firebase-auth-ktx")
    // "googleplayImplementation"("com.google.firebase:firebase-analytics-ktx")
    
    // ViewModel & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.8.6")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    // Debug tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
    
    // Additional kapt options to help with compilation issues
    arguments {
        arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
}

 