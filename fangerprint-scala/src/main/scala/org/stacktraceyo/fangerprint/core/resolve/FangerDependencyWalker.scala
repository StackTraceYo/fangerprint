package org.stacktraceyo.fangerprint.core.resolve

import org.stacktraceyo.fangerprint.core.FangerResolverConfiguration

import scala.collection.mutable
import scala.tools.asm._

/**
  * Created by Stacktraceyo on 7/3/17.
  */


class FangerDependencyWalker(asmVersion: Int, config: FangerResolverConfiguration) extends TypeRecorder {

  def resolveDependencies(): mutable.TreeMap[String, ClassReader] = {
    recordType(Type.getType(config.rootClass))
    while (pendingDependencies.nonEmpty) {
      val currentClassReader = pendingDependencies.remove(0)
      currentClassReader.accept(classVisitor, 0)
    }
    val sortedDependencies = new mutable.TreeMap[String, ClassReader]()
    sortedDependencies.++(dependencies)
    sortedDependencies
  }

  private val classVisitor = new ClassVisitor(asmVersion) {

    override def visit(version: Int, access: Int, name: String, signature: String, superName: String, interfaces: Array[String]): Unit = {
      recordTypeFromInternalName(superName)
      recordTypes(interfaces)
    }

    override def visitTypeAnnotation(typeRef: Int, typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor = {
      recordType(
        getTypeFromDesc(desc)
      )
      annotationVisitor
    }

    override def visitInnerClass(name: String, outerName: String, innerName: String, access: Int): Unit = {
      recordTypeFromInternalName(outerName)
      recordTypeFromInternalName(innerName)
    }

    override def visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array[String]): MethodVisitor = {
      recordTypes(exceptions)
      recordType(
        getMethodTypeFromDesc(desc)
      )
      methodVisitor
    }

    override def visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor = {
      recordType(
        getTypeFromDesc(desc)
      )
      annotationVisitor
    }

    override def visitField(access: Int, name: String, desc: String, signature: String, value: scala.Any): FieldVisitor = {
      recordType(
        getTypeFromDesc(desc)
      )
      fieldVisitor
    }
  }

  private val annotationVisitor = new AnnotationVisitor(asmVersion) {

    override def visitAnnotation(name: String, desc: String): AnnotationVisitor = {
      recordType(getTypeFromDesc(desc))
      this
    }

    override def visitEnum(name: String, desc: String, value: String): Unit = {
      recordType(getTypeFromDesc(desc))
    }
  }

  private val methodVisitor = new MethodVisitor(asmVersion) {
    override def visitTypeAnnotation(typeRef: Int, typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor = {
      recordType(getTypeFromDesc(desc))
      annotationVisitor
    }

    override def visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean): Unit = {
      recordTypes(owner)
      recordType(getTypeFromDesc(desc))

    }

    override def visitTryCatchBlock(start: Label, end: Label, handler: Label, `type`: String): Unit = {
      recordTypes(`type`)
    }

    override def visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor = {
      recordType(getTypeFromDesc(desc))
      annotationVisitor
    }

    override def visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor = {
      recordType(getTypeFromDesc(desc))
      annotationVisitor
    }

    override def visitMultiANewArrayInsn(desc: String, dims: Int): Unit = {
      recordType(getTypeFromDesc(desc))
    }

    override def visitTypeInsn(opcode: Int, `type`: String): Unit = {
      recordTypes(`type`)
    }

    override def visitFrame(`type`: Int, nLocal: Int, local: Array[AnyRef], nStack: Int, stack: Array[AnyRef]): Unit = {
      visitLocals(local)
      visitLocals(stack)
    }

    override def visitLocalVariable(name: String, desc: String, signature: String, start: Label, end: Label, index: Int): Unit = {
      recordType(getTypeFromDesc(desc))
    }

    override def visitLocalVariableAnnotation(typeRef: Int, typePath: TypePath, start: Array[Label], end: Array[Label], index: Array[Int], desc: String, visible: Boolean): AnnotationVisitor = {
      recordType(getTypeFromDesc(desc))
      annotationVisitor
    }

    override def visitTryCatchAnnotation(typeRef: Int, typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor = {
      recordType(getTypeFromDesc(desc))
      annotationVisitor
    }

    override def visitAnnotationDefault(): AnnotationVisitor = {
      annotationVisitor
    }

    override def visitFieldInsn(opcode: Int, owner: String, name: String, desc: String): Unit = {
      recordTypes(owner)
      recordType(getTypeFromDesc(desc))
    }
  }

  private val fieldVisitor = new FieldVisitor(asmVersion) {
    override def visitTypeAnnotation(typeRef: Int, typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor = {
      recordType(getTypeFromDesc(desc))
      annotationVisitor
    }

    override def visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor = {
      recordType(getTypeFromDesc(desc))
      annotationVisitor
    }
  }

  private def visitLocals(locals: Array[Object]) = {
    recordTypes(locals
      .map {
        case s: String =>
          s.toString
        case _ => null
      }.filter(_ != null)
    )
  }


}
