package markets

import akka.actor.{ActorRef, ActorSystem, Props}


trait BenchmarkSimulation {

  def system: ActorSystem

  /* Setup the markets. */
  def actorOf(props: Props): ActorRef = {
    system.actorOf(props)
  }

  def actorOf(props: Props, name: String): ActorRef = {
    system.actorOf(props, name)
  }

  def actorsOf(props: Iterable[Props]) = {
    props.map(prop => actorOf(prop))
  }

  def actorsOf(props: Iterable[Props], names: Iterable[String]): Iterable[ActorRef] = {
    props.zip(names).map{ case (prop, name) => actorOf(prop, name) }
  }

}
