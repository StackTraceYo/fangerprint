package org.stacktraceyo.fangerprint.core.resolve

import java.io.IOException

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.tools.asm.{ClassReader, Type}

/**
  * Created by Stacktraceyo on 7/3/17.
  */
trait TypeRecorder {

  val dependencies: mutable.Map[String, ClassReader] = scala.collection.mutable.HashMap[String, ClassReader]()
  val pendingDependencies: ListBuffer[ClassReader] = scala.collection.mutable.ListBuffer[ClassReader]()

  def recordTypes(types: Array[String]): Unit = {
    if (types != null) {
      types
        .foreach(typ => {
          recordTypeFromInternalName(typ)
        })

    }
  }

  def recordTypes(types: String*): Unit = {
    types
      .foreach(typ => {
        recordTypeFromInternalName(typ)
      })
  }

  def recordTypes(objectTypes: Array[Type]): Unit = {
    objectTypes
      .foreach(typ => {
        recordType(typ)
      })
  }


  def recordTypeFromInternalName(internalName: String): Unit = {
    if (internalName != null) {
      val objectType = Type.getObjectType(internalName)
      recordType(objectType)
    }
  }

  def recordType(objectType: Type): Unit = {
    objectType.getSort match {

      case Type.ARRAY =>
        recordType(objectType.getElementType)
      case Type.OBJECT =>
        val objName = objectType.getInternalName
        val className = objectType.getClassName
        if (!dependencies.contains(objName)) {
          try {
            val reader = new ClassReader(className)
            pendingDependencies += reader
            dependencies += objName -> reader
          } catch {
            case io: IOException =>
          }
        }
      case Type.METHOD =>
        recordType(objectType.getReturnType)
        recordTypes(objectType.getArgumentTypes)
      case _ =>
    }
  }

  def getTypeFromDesc(desc: String): Type = {
    Type.getType(desc)
  }

  def getMethodTypeFromDesc(desc: String): Type = {
    Type.getMethodType(desc)
  }

  def getTypeForLocal(local: String): Type = {
    Type.getObjectType(local)
  }


}
