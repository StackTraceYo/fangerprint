package org.stacktraceyo.fangerprint.core

import com.google.common.hash.{HashFunction, Hashing}

/**
  * Created by Stacktraceyo on 7/3/17.
  */

case class FangerHashingConfiguration(hashingFunction: HashFunction = Hashing.md5)
