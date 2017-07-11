package org.stacktraceyo.fangerprint.core

/**
  * Created by Stacktraceyo on 7/3/17.
  */
case class FangerResolverConfiguration(rootClass: Class[_], classNamesToIgnore: Set[String] = Set.empty, jarIgnoreClasses: Set[String] = Set.empty, ignoreJava: Boolean = false, ignoreScala: Boolean = false)



