package wowchat

import java.util.concurrent.{Executors, TimeUnit}

import wowchat.common.{CommonConnectionCallback, Global, ReconnectDelay, WowChatConfig}
import wowchat.discord.Discord
import wowchat.game.GameConnector
import wowchat.realm.{RealmConnectionCallback, RealmConnector}
import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.nio.NioEventLoopGroup

import scala.io.Source

object WoWChat extends StrictLogging {

  private val RELEASE = "v1.3.8-t1.17.1-2"

  def main(args: Array[String]): Unit = {
    logger.info(s"Running WoWChat - $RELEASE")
    val confFile = if (args.nonEmpty) {
      args(0)
    } else {
      logger.info("No configuration file supplied. Trying with default wowchat.conf.")
      "wowchat.conf"
    }
    Global.config = WowChatConfig(confFile)

    val gameConnectionController: CommonConnectionCallback = new CommonConnectionCallback {

      private val reconnectExecutor = Executors.newSingleThreadScheduledExecutor
      private val reconnectDelay = new ReconnectDelay

      override def connect: Unit = {
        Global.group = new NioEventLoopGroup

        val realmConnector = new RealmConnector(new RealmConnectionCallback {
          override def success(host: String, port: Int, realmName: String, realmId: Int, sessionKey: Array[Byte]): Unit = {
            gameConnect(host, port, realmName, realmId, sessionKey)
          }

          override def disconnected: Unit = doReconnect

          override def error: Unit = sys.exit(1)
        })

        realmConnector.connect
      }

      private def gameConnect(host: String, port: Int, realmName: String, realmId: Int, sessionKey: Array[Byte]): Unit = {
        new GameConnector(host, port, realmName, realmId, sessionKey, this).connect
      }

      override def connected: Unit = reconnectDelay.reset

      override def disconnected: Unit = doReconnect

      def doReconnect: Unit = {
        Global.group.shutdownGracefully()
        Global.discord.changeRealmStatus("Connecting...")
        val delay = reconnectDelay.getNext
        logger.info(s"Disconnected from server! Reconnecting in $delay seconds...")

        reconnectExecutor.schedule(new Runnable {
          override def run(): Unit = connect
        }, delay, TimeUnit.SECONDS)
      }
    }

    logger.info("Connecting to Discord...")
    Global.discord = new Discord(new CommonConnectionCallback {
      override def connected: Unit = gameConnectionController.connect

      override def error: Unit = sys.exit(1)
    })
  }
}
