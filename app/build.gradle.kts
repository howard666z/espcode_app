import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


plugins {
    alias(libs.plugins.android.application)
}


android {
    namespace = "com.example.espcode_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.espcode_app"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(project(":secret_src"))
}



/* -------- helper：簽章 SHA-256 → 32-byte key -------- */
fun deriveKey32(): ByteArray {
    // 讀 release keystore 資訊（按你專案 signingConfig 調整）
    val storeFile = File(System.getProperty("user.home") + "/.android/debug.keystore")
    val storePwd  = "android".toCharArray()
    val alias     = "androiddebugkey"
    val keyPwd    = "android".toCharArray()


    val ks = KeyStore.getInstance("JKS").apply {
        storeFile.inputStream().use { load(it, storePwd) }
    }

    val cert = ks.getCertificate(alias)
        ?: error("Certificate $alias not found in keystore")
    val sha  = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
    return sha.copyOf(32)
}

/* -------- 主要 Task -------- */

// 將以下 block 搬到 app module 的 build.gradle.kts 中執行
if (project.name == "app") {
    tasks.register("encryptSecret") {
        doLast {
            /* ❶ 自動尋找 classes.jar 與 libloop.so */
            val secretBuildDir = rootProject.file("secret_src/build")
            val classDir = File(rootProject.projectDir, "secret_src/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes")
            val jarFile = File(buildDir, "tmp/classes.jar").apply { parentFile.mkdirs() }

            ZipOutputStream(FileOutputStream(jarFile)).use { zos ->
                classDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val entryName = file.relativeTo(classDir).path.replace(File.separatorChar, '/')
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().copyTo(zos)
                    zos.closeEntry()
                }
            }

            val soFile: File? = null // 不使用 native lib


            /* ❷ 打 zip */
            val tmpZip = File(buildDir, "tmp/secret.zip").apply { parentFile.mkdirs() }
            ZipOutputStream(FileOutputStream(tmpZip)).use { zos ->
                listOf(jarFile).forEach { f ->
                    zos.putNextEntry(ZipEntry(f.name))
                    f.inputStream().copyTo(zos, 4096)
                    zos.closeEntry()
                }
            }

            /* ❸ AES-GCM 加密 */
            val key = deriveKey32()
            val iv  = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(
                    Cipher.ENCRYPT_MODE,
                    SecretKeySpec(key, "AES"),
                    GCMParameterSpec(128, iv)
                )
            }
            val enc = cipher.doFinal(tmpZip.readBytes())

            /* ❹ 寫入 assets/secret.bin（絕對路徑修正） */
            val outBin = File(project.projectDir, "src/main/assets/secret.bin")
            outBin.parentFile.mkdirs()
            outBin.writeBytes(iv + enc)
            println("✓ secret.bin 生成於 ${outBin.absolutePath}，大小：${outBin.length()} bytes")

            /* ❺ 清理暫存與明文 jar/so */
            tmpZip.delete()
            jarFile.delete()
        }
    }

    /* 讓 preBuild 依賴此任務 */
    tasks.named("encryptSecret").configure {
        dependsOn(project(":secret_src").tasks.named("assemble"))
    }


    tasks.named("preBuild").configure {
        dependsOn("encryptSecret")
    }
}

