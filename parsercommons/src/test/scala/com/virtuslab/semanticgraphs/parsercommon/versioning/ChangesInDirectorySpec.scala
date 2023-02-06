package com.virtuslab.semanticgraphs.parsercommon.versioning

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Files

class ChangesInDirectorySpec extends AnyWordSpec {
  "hasChanged" should {
    "detect that file has changed since last stamp" in {
      val projectDirectory = Files.createTempDirectory("GB")
      val filePath         = projectDirectory.resolve("file.java")
      filePath.toFile.createNewFile()
      FileVersionFromFile.saveVersionOfFile(projectDirectory, filePath)
      Files.write(filePath, "Content".getBytes)
      assert(ChangesInDirectory.hasChanged(projectDirectory, filePath))
    }
  }

  "areChanges" should {
    "correctly indicate changes in project" when {
      val projectDirectory = Files.createTempDirectory("GB")
      val filePath         = projectDirectory.resolve("file.java")
      filePath.toFile.createNewFile()
      "there are changes" in {
        FileVersionFromFile.saveVersionOfFile(projectDirectory, filePath)
        Files.write(filePath, "Content".getBytes)
        assert(ChangesInDirectory.areChanges(projectDirectory.toString, ".java"))
      }
      "there is no changes" in {
        FileVersionFromFile.saveVersionOfFile(projectDirectory, filePath)
        assert(!ChangesInDirectory.areChanges(projectDirectory.toString, ".java"))
      }
    }
  }
}
