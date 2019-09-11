package com.github.caeus.sumba

import org.scalatest._

import scala.beans.BeanProperty

@With
case class Perro(@BeanProperty hi:List[String],he:String,ho:Perro){
  def this()=this(null,null,null)
}

class HelloSpec extends FlatSpec with Matchers {
  "With annotation " should "work" in {
    Perro(null,null,null).withHi(Nil).hi shouldEqual Nil

  }
}
