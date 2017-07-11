package org.stacktraceyo.fangerprint.core

import org.scalatest.FunSpec

class FangerTest extends FunSpec {
  describe("Fanger.resolve") {
    it("should return some string for a dependency") {
      val fanger = new Fanger(classOf[Fanger])
      fanger.compute()
    }
  }
}