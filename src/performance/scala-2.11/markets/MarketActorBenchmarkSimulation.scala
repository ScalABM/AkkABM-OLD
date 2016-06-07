package markets

import akka.actor.{ActorRef, Props}
import akka.agent.Agent
import akka.routing.{ActorRefRoutee, Broadcast, RoutingLogic}

import markets.actors.participants.Add
import markets.actors.participants.issuers.{TestRandomOrderIssuer, TestRandomOrderIssuerConfig}
import markets.tickers.Tick
import markets.tradables.Tradable
import org.apache.commons.math3.random.MersenneTwister

import scala.concurrent.ExecutionContext


/** Simulation used to benchmark a specific implementation of a market actor. */
trait MarketActorBenchmarkSimulation extends BenchmarkSimulation {

  import MarketActorBenchmarkSimulation._

  def numberMarkets: Int

  def numberOrderIssuers: Int

  def orderIssuerConfig: TestRandomOrderIssuerConfig

  def prng: MersenneTwister

  def routingLogic: RoutingLogic

  /** Not happy with this! */
  def getMarketProps(referencePrice: Long,
                     settlementMechanismRef: ActorRef,
                     ticker: Agent[Tick],
                     tradable: Tradable): Props

  /* Create a router that uses order issuers as routees. */
  private[this] val orderIssuerRefs = for (i <- 1 to numberOrderIssuers) yield {
    val rng = new MersenneTwister(prng.nextLong())
    val orderIssuerProps = TestRandomOrderIssuer.props(rng, orderIssuerConfig)
    actorOf(orderIssuerProps)
  }
  private[this] val routees = orderIssuerRefs.map(orderIssuerRef => ActorRefRoutee(orderIssuerRef))
  val brokerage = actorOf(TestBrokerageActor.props(routingLogic, routees))

  /* Create some markets. */
  private[this] val maximumBidPrice = {
    orderIssuerConfig.bidOrderIssuingStrategyConfig.tradingStrategyConfig.maximumPrice
  }
  private[this] val referencePrices = Vector.fill(numberMarkets)(prng.nextLong(maximumBidPrice))
  private[this] val tickers = referencePrices.map {
    case referencePrice =>
      val price = prng.nextLong(referencePrice)
      val askPrice = prng.nextLong(price)
      val bidPrice = price + prng.nextLong(maximumBidPrice - price)
      val quantity = prng.nextLong(maximumQuantity)
      ticker(askPrice, bidPrice, price, quantity, System.currentTimeMillis())(system.dispatcher)
  }
  private[this] val tradables = for (i <- 1 to numberMarkets) yield Tradable(i.toString)

  private[this] val marketRefs = initialTicks.map {
    case initialTick =>
      val marketProps = MarketActor.props[CC1, CC2](system.deadLetters, initialTick)
      actorOf(marketProps)
  }

  marketRefs.foreach {
    case marketRef => brokerage ! Broadcast(Add(marketRef))
  }

  /* Setup the MarketRegulatorActor. */
  val marketRegulator = actorOf(MarketRegulatorActor.props(orderIssuerRefs, marketRefs))

  private[this] val maximumQuantity = {
    val maximumAskQuantity = orderIssuerConfig.askOrderIssuingStrategyConfig.tradingStrategyConfig.maximumQuantity
    val maximumBidQuantity = orderIssuerConfig.bidOrderIssuingStrategyConfig.tradingStrategyConfig.maximumQuantity
    Math.max(maximumAskQuantity, maximumBidQuantity)
  }

}


object MarketActorBenchmarkSimulation {

  def ticker(askPrice: Long,
             price: Long,
             bidPrice: Long,
             quantity: Long,
             timestamp: Long)(ec: ExecutionContext): Agent[Tick] = {
    Agent(Tick(askPrice, bidPrice, price, quantity, timestamp))(ec)
  }

}
