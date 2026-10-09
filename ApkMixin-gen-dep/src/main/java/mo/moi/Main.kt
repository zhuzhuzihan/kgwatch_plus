package mo.moi

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Generic stub generator: rewrites a dex2jar-merged raw.jar of ANY target APK
 * into a compile-only source.jar.
 *
 * - Classes/inners/methods/fields: strip final, open visibility (public).
 * - Private <init> becomes public so @Mixin subclasses can call it.
 * - Drops Kotlin Metadata + RestrictTo annotations that confuse D8.
 *
 * Usage:
 *   1. dex2jar the target APK -> ApkMixin-gen-dep/raw.jar (merge all dex if multidex)
 *   2. Run this (JVM): `./gradlew -p ApkMixin-gen-dep run` or via IDE,
 *      optionally with args: `raw.jar app/libs/source.jar`
 *   3. app/ compiles against app/libs/source.jar (compileOnly).
 */
fun main(args: Array<String>) {
    val inputJar = File(args.getOrElse(0) { "./ApkMixin-gen-dep/raw.jar" })
    val outputJar = File(args.getOrElse(1) { "./app/libs/source.jar" })
    require(inputJar.isFile) { "raw.jar not found: ${inputJar.absolutePath} (dex2jar the target APK first)" }
    outputJar.parentFile.mkdirs()
    processJar(inputJar, outputJar)
    println("Wrote ${outputJar.absolutePath}")
}

val excluded = arrayOf("kotlin", "androidx")
fun processJar(inputJar: File, outputJar: File) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(outputJar))).use { zos ->
        ZipInputStream(BufferedInputStream(FileInputStream(inputJar))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = entry.name
                    val bytes = zis.readBytes()
                    val modifiedBytes =
                        if (entryName.endsWith(".class") && !excluded.any { entryName.startsWith(it) }) {
                            modifyClass(bytes)
                        } else {
                            bytes
                        }
                    zos.putNextEntry(ZipEntry(entryName))
                    zos.write(modifiedBytes)
                    zos.closeEntry()
                }
                entry = zis.nextEntry
            }
        }
    }
}

fun modifyClass(classData: ByteArray): ByteArray {
    val reader = ClassReader(classData)
    val writer = ClassWriter(reader, 0)
    val visitor = object : ClassVisitor(Opcodes.ASM9, writer) {
        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            val newAccess = (access and Opcodes.ACC_FINAL.inv()) or Opcodes.ACC_PUBLIC
            super.visit(version, newAccess, name, signature, superName, interfaces)
        }

        override fun visitInnerClass(name: String?, outerName: String?, innerName: String?, access: Int) {
            var newInnerName = innerName
            var newOuterName = outerName
            if (innerName == null && outerName == null && name != null) {
                newInnerName = name.substringAfterLast("/")
                newOuterName = name.substringBeforeLast("/") + "/" + newInnerName.substringBefore("$")
            }
            super.visitInnerClass(
                name, newOuterName, newInnerName,
                access and Opcodes.ACC_PRIVATE.inv()
                    and Opcodes.ACC_FINAL.inv()
                    or Opcodes.ACC_PUBLIC
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
                newAccess = newAccess or Opcodes.ACC_PUBLIC
                newAccess = newAccess and Opcodes.ACC_PRIVATE.inv()
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
            val newAccess = access and Opcodes.ACC_FINAL.inv()
            return super.visitField(newAccess, name, descriptor, signature, value)
        }

        val removed = arrayOf("Lkotlin/Metadata;", "Landroidx/annotation/RestrictTo;")
        override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
            return if (removed.contains(descriptor)) null
            else super.visitAnnotation(descriptor, visible)
        }
    }
    reader.accept(visitor, 0)
    return writer.toByteArray()
}
