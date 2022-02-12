package org.virtuslab.semanticgraphs.analytics.utils

import java.nio.file.Paths
import java.nio.file.Path

// /**
//   * Contains some helper methods to handle paths in type [[String]] as normal [[Path]].
//   * @param path
//   *   path which is converted.
//   */
// extension (path: String)

//   /**
//     * Convert path string to [[java.nio.file.Path]] and execute [[java.nio.file.Path resolve(other)]].
//     */
//   def resolve(other: String): Path =
//     Paths.get(path).normalize().resolve(other)

object PathHelpers {
  implicit class StringResolver(path: String) {
    def resolve(other: String): Path =
      Paths.get(path).normalize().resolve(other)
  }

}
