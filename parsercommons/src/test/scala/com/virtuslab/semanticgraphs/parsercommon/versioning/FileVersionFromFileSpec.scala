package com.virtuslab.semanticgraphs.parsercommon.versioning

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.{ Files, Path, Paths }
import scala.io.Source

class FileVersionFromFileSpec extends AnyFlatSpec with Matchers {
  private val exampleFolderFile = Paths.get(getClass.getClassLoader.getResource("exampleFolder").getPath).toFile

  "FileVersionFromFile.getForFilesInDirectory" should "return correctly files from folder" in {
    FileVersionFromFile.getForFilesInDirectory(exampleFolderFile, ".java").size shouldBe 1
  }

  "FileVersion" should "allow to detect changes in directory" in {
    val ver1 = FileVersionFromFile.getForFilesInDirectory(exampleFolderFile, ".java").map(_.toStamp)

    val file = exampleFolderFile.toPath.resolve("newFile.java").toFile
    file.createNewFile()

    val ver2 = FileVersionFromFile.getForFilesInDirectory(exampleFolderFile, ".java").map(_.toStamp)

    file.delete()

    val ver3 = FileVersionFromFile.getForFilesInDirectory(exampleFolderFile, ".java").map(_.toStamp)

    ver1 shouldBe ver3
    ver2.toSet.diff(ver1.toSet) should not be empty
  }

  it should "allow to detect changes in files" in {
    val file = exampleFolderFile.toPath.resolve("newFile.java").toFile
    file.createNewFile()

    val ver1 = FileVersionFromFile.getForFilesInDirectory(exampleFolderFile, ".java").map(_.toStamp)

    Files.write(file.toPath, "Content".getBytes)

    val ver2 = FileVersionFromFile.getForFilesInDirectory(exampleFolderFile, ".java")

    val versionsDiff = ver2.toSet.diff(ver1.toSet)

    file.delete()

    versionsDiff should not be empty
    versionsDiff.exists(_.path == file.getPath)
  }
}
