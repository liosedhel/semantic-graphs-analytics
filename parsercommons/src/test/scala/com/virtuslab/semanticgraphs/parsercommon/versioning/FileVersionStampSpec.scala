package com.virtuslab.semanticgraphs.parsercommon.versioning

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class FileVersionStampSpec extends AnyFlatSpec with Matchers {
  "Json conversion" should "works correctly" in {
    val fileVersionStampSeq = Seq(FileVersionStamp("path", Long.MaxValue, Long.MinValue, 0))
    FileVersionStamp.readSeq(FileVersionStamp.writeSeq(fileVersionStampSeq)) mustBe fileVersionStampSeq
  }
}
