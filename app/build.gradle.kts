import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

buildscript {
    dependencies {
        // Used by the patchStubJar task below to rewrite the compile-only stub jar.
        classpath("org.ow2.asm:asm:9.9")
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("momoi.plugin.apkmixin") apply true
}

// The stub jar (app/libs/source.jar) is compile-only and never ships in the APK.
// It is a plain dex2jar merge of the target, so target classes may be final /
// package-private with private ctors — unusable as @Mixin supertypes. The build
// compiles against a patched copy rewritten here with ASM (same opening rules as
// ApkMixin-gen-dep's MainKt, applied at build time so a raw stub always works):
//   - classes/inners/methods/fields: strip final, open visibility (public);
//   - private <init> becomes public so @Mixin subclasses can call it;
//   - EnclosingMethod attribute dropped (keep InnerClasses): D8 rejects a class
//     carrying BOTH ("a member class cannot also be a non-member local class");
//   - Kotlin Metadata + RestrictTo annotations dropped (they confuse D8).
// Real classes come from the patched target APK at runtime — this only affects compilation.
val patchedStubJar = layout.buildDirectory.file("patched-libs/source.jar")
val patchStubJar = tasks.register("patchStubJar") {
    val srcJar = file("libs/source.jar")
    inputs.file(srcJar)
    outputs.file(patchedStubJar)
    doLast {
        val out = patchedStubJar.get().asFile
        out.parentFile.mkdirs()
        val excluded = arrayOf("kotlin", "androidx")
        ZipFile(srcJar).use { zip ->
            ZipOutputStream(out.outputStream().buffered()).use { zos ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val e = entries.nextElement()
                    val bytes = zip.getInputStream(e).readBytes()
                    val outBytes = if (e.name.endsWith(".class") && !excluded.any { e.name.startsWith(it) }) {
                        val cr = ClassReader(bytes)
                        val cw = ClassWriter(cr, 0)
                        cr.accept(object : ClassVisitor(Opcodes.ASM9, cw) {
                            // Drop the EnclosingMethod attribute (keep InnerClasses) → pure member class.
                            override fun visitOuterClass(owner: String?, name: String?, descriptor: String?) {}

                            override fun visit(
                                version: Int,
                                access: Int,
                                name: String?,
                                signature: String?,
                                superName: String?,
                                interfaces: Array<out String>?
                            ) {
                                super.visit(
                                    version,
                                    (access and Opcodes.ACC_FINAL.inv()) or Opcodes.ACC_PUBLIC,
                                    name, signature, superName, interfaces
                                )
                            }

                            override fun visitInnerClass(
                                name: String?,
                                outerName: String?,
                                innerName: String?,
                                access: Int
                            ) {
                                var newInnerName = innerName
                                var newOuterName = outerName
                                if (innerName == null && outerName == null && name != null) {
                                    newInnerName = name.substringAfterLast("/")
                                    newOuterName = name.substringBeforeLast("/") + "/" + newInnerName.substringBefore("$")
                                }
                                super.visitInnerClass(
                                    name, newOuterName, newInnerName,
                                    access and Opcodes.ACC_PRIVATE.inv() and
                                        Opcodes.ACC_FINAL.inv() or Opcodes.ACC_PUBLIC
                                )
                            }

                            override fun visitMethod(
                                access: Int,
                                name: String?,
                                descriptor: String?,
                                signature: String?,
                                exceptions: Array<out String>?
                            ): MethodVisitor {
                                var newAccess = access and Opcodes.ACC_FINAL.inv()
                                if (name == "<init>" && (access and Opcodes.ACC_PRIVATE) != 0) {
                                    newAccess = newAccess or Opcodes.ACC_PUBLIC and
                                        Opcodes.ACC_PRIVATE.inv()
                                }
                                return super.visitMethod(newAccess, name, descriptor, signature, exceptions)
                            }

                            override fun visitField(
                                access: Int,
                                name: String?,
                                descriptor: String?,
                                signature: String?,
                                value: Any?
                            ): FieldVisitor {
                                return super.visitField(
                                    access and Opcodes.ACC_FINAL.inv(),
                                    name, descriptor, signature, value
                                )
                            }

                            val removed = arrayOf("Lkotlin/Metadata;", "Landroidx/annotation/RestrictTo;")
                            override fun visitAnnotation(
                                descriptor: String?,
                                visible: Boolean
                            ): AnnotationVisitor? {
                                return if (removed.contains(descriptor)) null
                                else super.visitAnnotation(descriptor, visible)
                            }
                        }, 0)
                        cw.toByteArray()
                    } else {
                        bytes
                    }
                    zos.putNextEntry(ZipEntry(e.name))
                    zos.write(outBytes)
                    zos.closeEntry()
                }
            }
        }
    }
}

android {
    namespace = "momoi.mod.kgwatch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kugou.android.watch.lite"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":ApkMixin-annotation"))
    compileOnly(libs.androidx.appcompat)
    // Compile against the build-time-opened stub jar (see patchStubJar above).
    compileOnly(files(patchStubJar))
    compileOnly(libs.androidx.fragment)
    compileOnly(libs.androidx.constraintlayout)
    compileOnly(libs.androidx.recyclerview)
    compileOnly(libs.androidx.viewpager2)
    compileOnly(libs.androidx.core)
}

apkMixin {
    versionName = "1.3"
    targetApk = "source.apk"
    // Generic ApkMixin fields: app-specific identity moved out of the plugin.
    applicationId = "com.kugou.android.watch.lite"
    appLabel = "KG Watch Plus"
    useProcessorCountAsThreadCount = project.properties["useProcessorCountAsThreadCount"] == "true"

    signing {
        keyFile = file("mixin/testkey.pk8")
        certFile = file("mixin/testkey.x509.pem")
    }

    output {
        signedFileName = "KGWatchPlus_${versionName}.apk"
    }
}
