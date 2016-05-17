package controllers

import akka.actor._

object Watermarker {
  def props = Props[Watermarker]
}

class Watermarker extends Actor {
  def receive = {
    case id: String =>
      println(id)
  }
}
