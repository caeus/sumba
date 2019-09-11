package com.github.caeus.sumba

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

@compileTimeOnly("Enable macro paradise to expand compile-time macros")
class With extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro WithMacros.impl
}

object WithMacros {

  def impl(c: blackbox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    case class SimplParam(name: TermName, tpe: Tree) {
      def isSame(other: SimplParam): Boolean = {
        name == other.name && tpe.toString() == other.tpe.toString
      }
    }
    case class SimplCtor(params: List[SimplParam]) {
      def isSame(other: SimplCtor): Boolean = {
        params.size == other.params.size && params.zip(other.params).forall(t => t._1 isSame t._2)
      }
    }
    annottees.head.tree match {
      case ClassDef(mods, tpname, tparams, templ@Template(parents, self, body: Seq[Tree])) if mods.hasFlag(Flag.CASE)=>
      case _ => c.abort(annottees.head.tree.pos, "Can only annotate case classes")
    }

    val modifiedTrees: Seq[c.universe.Tree] = annottees.map(_.tree).map {
      case clzzDef@ClassDef(mods, tpname, tparams, templ@Template(parents, self, body: Seq[Tree])) if mods.hasFlag(Flag.CASE) =>
        val fields: SimplCtor = SimplCtor(body.collect {
          case ValDef(memMods, name, tpe, _) if memMods.hasFlag(Flag.CASEACCESSOR | Flag.PARAMACCESSOR) =>
            SimplParam(name, tpe)
        })
        val constructors: Seq[SimplCtor] = body.collect {
          case method@DefDef(_, methodName, _, vparams, _, _) if methodName.toString == "<init>" =>
            SimplCtor(
              vparams.head.map {
                case ValDef(_, name: TermName, tpe: Tree, _) =>
                  SimplParam(name, tpe)
              })
        }

        if (constructors.exists(_.isSame(fields))) {
          val withers = fields.params.map {
            param: SimplParam =>
              q"""def ${TermName("with" + param.name.toString.capitalize)}(value:${param.tpe}) : $tpname = this.copy(${TermName(param.name.toString)}= value)"""
          }
          ClassDef(mods, tpname, tparams, Template(parents, self,
            q"""
              ..$body
              ..$withers
         """.children))
        } else {
          c.abort(clzzDef.pos,"Could not be identified as a case class")
        }


      case x => x

    }
    c.Expr[Any](q"{..$modifiedTrees}")

  }
}

