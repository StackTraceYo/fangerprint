package org.stacktraceyo.fangerprint.core.fingerprint

import org.stacktraceyo.fangerprint.core.FangerHashingConfiguration

import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.tools.asm.ClassReader

/**
  * Created by Stacktraceyo on 7/3/17.
  */
class FangerPrinter(config: FangerHashingConfiguration) {

  def createHashOfDependencies(dependencies: mutable.TreeMap[String, ClassReader]): String = {
    val hasher = config.hashingFunction.newHasher()
    dependencies
      .filter(dep => dep._2 != null)
      .foreach(dep => hasher.putBytes(dep._2.b))
    val hash = hasher.hash().toString
    hash
  }


  private def toDotNotation(name: String): String = {
    convertName(name)
  }

  private def fromDotNotation(name: String): String = {
    convertName(name, isDotNotation = true)

  }

  private def convertName(name: String, isDotNotation: Boolean = false): String = {
    if (isDotNotation) name.replace('.', '/') else name.replace('/', '.')
  }

  //  private def getClassSource(name: String): URL = {
  //    var loader = this.getClass.getClassLoader
  //    var source: URL = null
  //    try {
  //      loader.loadClass(toDotNotation(name)).getProtectionDomain.getCodeSource.getLocation
  //    }
  //    catch {
  //      case e: Exception =>
  //        loader = ClassLoader.getSystemClassLoader
  //        while (loader != null) {
  //          try {
  //            source = loader.loadClass(toDotNotation(name)).getProtectionDomain.getCodeSource.getLocation
  //          }
  //          catch {
  //            case e: Exception =>
  //              loader = loader.getParent
  //          }
  //        }
  //    }
  //    source
  //  }

}
