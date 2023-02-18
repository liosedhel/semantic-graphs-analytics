package com.virtuslab.semanticgraphs.parsercommon.versioning

import com.virtuslab.semanticgraphs.parsercommon.*

import java.nio.file.Path

case class ChangesInDirectory(newOrModifiedFiles: Seq[Path], deletedFiles: Seq[Path]) {
  def areChanges(): Boolean = newOrModifiedFiles.nonEmpty || deletedFiles.nonEmpty
}

object ChangesInDirectory {
  private def getOldState(projectPath: Path): Set[FileVersion] =
    FileVersionStamp.readState(projectPath).toSet[FileVersion]

  def changesInDirectory(
    projectPath: String,
    fileSuffix: String
  ): ChangesInDirectory = {
    val oldState = getOldState(projectPath.toPath)
    val newState = FileVersionFromFile.getForFilesInDirectory(projectPath.toPath.toFile, fileSuffix).toSet[FileVersion]
    val newOrModifiedFiles = newState.diff(oldState).toSeq
    val deletedFiles = oldState.diff(newState).toSeq
    ChangesInDirectory(
      newOrModifiedFiles.map(_.path.toPath),
      deletedFiles.map(_.path.toPath)
    )
  }

  def areChanges(projectPath: String, fileSuffix: String): Boolean =
    changesInDirectory(projectPath, fileSuffix).areChanges()

  def hasChanged(projectPath: Path, filePath: Path): Boolean = !getOldState(projectPath)
    .find(_.path == filePath.toString)
    .contains(FileVersionFromFile(filePath.toFile).asInstanceOf[FileVersion])

}
