plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.creator.eternalbonds"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.creator.eternalbonds"
        minSdk = 31
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }
    signingConfigs {
        create("release") {
            storeFile = file("../EternalBonds.jks")
            storePassword = "Wjh1318094164"
            keyAlias = "key0"
            keyPassword = "Wjh1318094164"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment:1.6.2")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("com.github.bumptech.glide:glide:4.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.google.code.gson:gson:2.10.1")

//    implementation("com.huawei.hms:ads-identifier:3.4.62.300")
//    implementation("com.huawei.hms:ads-installreferrer:3.4.62.300")
    implementation ("com.huawei.hms:ads-lite:13.4.68.300")
    /*--------------------------------------------------------------------*/
    implementation(project(":librarys"))
    implementation(project(":librarys:exoplayer"))
    implementation(project(":librarys:common"))
    implementation(project(":librarys:websocket"))
    implementation(project(":librarys:nanohttpd"))
    implementation(project(":librarys:remote_controller"))
}