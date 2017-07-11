package org.stacktraceyo.fangerprint.core

import org.stacktraceyo.fangerprint.core.fingerprint.FangerPrinter
import org.stacktraceyo.fangerprint.core.resolve.FangerDependencyWalker

import scala.tools.asm.Opcodes

/**
  * Created by Stacktraceyo on 7/3/17.
  */
class Fanger(classRoot: Class[_], asmVersion: Int = Opcodes.ASM5) {

  private val hashingConfiguration = FangerHashingConfiguration()
  private val resolverConfiguration = FangerResolverConfiguration(classRoot)
  private val fangerPrinter = new FangerPrinter(hashingConfiguration)
  private val depWalker = new FangerDependencyWalker(asmVersion, resolverConfiguration)

  def compute(): Unit = {
    println(
      fangerPrinter.createHashOfDependencies(
        depWalker.resolveDependencies()
      )
    )
  }


}
