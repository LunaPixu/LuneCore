package net.lunapixu.lunecore.luckperms

import net.luckperms.api.model.user.User
import net.lunapixu.lunecore.luckperms.MetaHelper
import net.lunapixu.lunecore.luckperms.MetaModifiedEvent.getHandlerList
import org.bukkit.event.{Event, HandlerList}
import org.bukkit.Bukkit

object MetaModifiedEvent:
  val handlerList: HandlerList = HandlerList()
  def getHandlerList = handlerList

class MetaModifiedEvent(private val user: User, private val metaType: String, private val oldNode: MetaHelper.OptionalWrapper, private val newNode: MetaHelper.Wrapper)
  extends Event:
  def getUser = user
  def getOldNode = oldNode
  def getNewNode = newNode
  def getMetaType = metaType

  def getUserAsPlayer = Bukkit.getServer.getPlayer(user.getUniqueId)

  def getHandlers(): HandlerList = getHandlerList
