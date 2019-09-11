package com.github.caeus.sumba

import org.scalatest._

import scala.beans.BeanProperty

@With
case class Perro(@BeanProperty hi:List[String],he:String,ho:Perro){
  def this()=this(null,null,null)
}

class HelloSpec extends FlatSpec with Matchers {
  "The Hello object" should "say hello" in {
    println(Perro(null,null,null).withHi(Nil))

  }
}
