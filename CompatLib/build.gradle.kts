plugins {
    id("com.android.application").version("7.2.1")
    kotlin("android").version("1.6.21")
}

android {
    namespace = "app.lawnchair.compatlib"
    compileSdk = 32

    defaultConfig {
        minSdk = 26
        targetSdk = 32

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        //consumerProguardFiles.add("consumer-rules.pro")
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )
        }
        create("neo") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = compileOptions.sourceCompatibility.toString()
    }

    addFrameworkJar("framework-12.jar")
}

dependencies {
    implementation("androidx.core:core-ktx:1+")
}

fun Project.addFrameworkJar(path: String) {
    val frameworkJar = File(rootProject.projectDir, "prebuilts/libs/$path")
    if (!frameworkJar.exists()) {
        throw IllegalArgumentException("Framework jar path doesn't exist")
    }
    gradle.projectsEvaluated {
        tasks.withType<JavaCompile> {
            options.bootstrapClasspath =
                files(listOf(frameworkJar) + (options.bootstrapClasspath?.files as Iterable<File>))
        }
        tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask> {
            classpath.from(files(listOf(frameworkJar) + (classpath.files as Iterable<File>)))
        }
    }
}