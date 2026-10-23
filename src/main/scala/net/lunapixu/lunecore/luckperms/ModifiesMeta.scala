package net.lunapixu.lunecore.luckperms

import java.util.concurrent.{CompletableFuture, TimeUnit}
import net.kyori.adventure.text.event.{ClickEvent, HoverEvent}
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.{Placeholder, TagResolver}
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.text.Component
import net.luckperms.api.cacheddata.Result
import net.luckperms.api.model.user.{User, UserManager}
import net.luckperms.api.node.types.{ChatMetaNode, MetaNode, PrefixNode, SuffixNode}
import net.luckperms.api.node.types.PrefixNode.Builder as PrefixBuilder
import net.luckperms.api.node.types.SuffixNode.Builder as SuffixBuilder
import net.luckperms.api.node.Node
import net.lunapixu.lunecore.luckperms.{MetaHelper, MetaModifiedEvent}
import net.lunapixu.lunecore.utils.StringUtils
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag
import scala.util.Try

abstract class ModifiesMeta(private val metaType: String):
  protected given configPath: String = s"commands.set-${metaType}"
  def getConfigPath = configPath
  def getMetaType = metaType

  protected val timeoutSecs = 20

  protected def updateUserMeta(
    userFuture: CompletableFuture[User],
    metaStr: String,
    nodeBuilder: MetaHelper.NodeBuilder
  )(using
    config: FileConfiguration
  )(using userManager: UserManager): CompletableFuture[MetaModifiedEvent] = userFuture
    .handleAsync { (user, t) =>
      if t != null then throw t
      val userMetadata = user.getCachedData.getMetaData

      val (metaNodes, defaultMeta) = nodeBuilder match
        case _: PrefixBuilder => (userMetadata.getPrefixes, "%s ")
        case _: SuffixBuilder => (userMetadata.getSuffixes, "%s")

      val highestWeight = metaNodes.entrySet.asScala.map { entry => (entry.getValue, entry.getKey) }
        .foldLeft(1) { (acc, p: MetaHelper.Tuple) => acc max p.weight }

      val metaPath = s"${getMetaType}-template"
      val rank = userMetadata.getPrimaryGroup
      val metaTemplate = Option(config.getString(s"${configPath}.${metaPath}.${rank}"))
        .getOrElse(config.getString(s"${configPath}.${metaPath}.default", defaultMeta))
      val formattedMeta = String.format(metaTemplate, metaStr)
      val metaNode: MetaHelper.Node = nodeBuilder.match
        case p: PrefixBuilder => p.prefix(formattedMeta)
        case s: SuffixBuilder => s.suffix(formattedMeta)
      .priority(highestWeight).build

      val cacheKey = s"lunecore.luckperms.plain${metaType}"
      val metaCache = MetaNode.builder(cacheKey, metaStr).build
      val oldCacheQuery = userMetadata.queryMetaValue(cacheKey)
      val (oldCache, oldCacheNode) = (Option(oldCacheQuery.result), Option(oldCacheQuery.node))

      // The only reason why this is not in the deconstructed tuple above is because type refinement just... breaks...?
      val nodeQuery: Result[String, PrefixNode] | Result[String, SuffixNode] = nodeBuilder match
        case p: PrefixBuilder => userMetadata.queryPrefix
        case s: SuffixBuilder => userMetadata.querySuffix
      val oldNode: Option[MetaHelper.Node] = Option(nodeQuery.node)

      oldNode.foreach { (n: Node) => user.data.remove(n) }
      user.data.add(metaNode)
      oldCacheNode.foreach { (n: Node) => user.data.remove(n) }
      user.data.add(metaCache)
      userManager.saveUser(user).get(timeoutSecs, TimeUnit.SECONDS)

      MetaModifiedEvent(
        user,
        metaType,
        MetaHelper.OptionalWrapper(oldCache, oldNode),
        MetaHelper.Wrapper(metaStr, metaNode)
      )
    }

  protected def makeUpdatedMetaComponent(event: MetaModifiedEvent): Component =
    val plainText = PlainTextComponentSerializer.plainText
    val mm = MiniMessage.miniMessage
    val oldNode = event.getOldNode
    val formerly = oldNode.node.fold("") { node =>
      s"\n  <grey><i>Was (</i></grey><formernode><grey><i>)."
    }
    // If this is a user's first time updating their meta, try to recover the old node text from raw data since cache will be non-existent
    val clipboardFormer = oldNode.rawString.getOrElse(oldNode.node.fold("") { _.getMetaValue.strip })
    event.getUserAsPlayer.displayName.append(mm.deserialize(
      s" <gold>has had their ${getMetaType} updated to (</gold><newnode><gold>)!${formerly}",
      Placeholder.component(
        "newnode",
        mm.deserialize(
          StringUtils.stringToMiniMessage(event.getNewNode.node.getMetaValue.strip),
          TagResolver.standard
        )
      ),
      Placeholder.component(
        "formernode",
        mm.deserialize(
          StringUtils.stringToMiniMessage(oldNode.node.fold("") { _.getMetaValue.strip }),
          TagResolver.standard
        ).clickEvent(ClickEvent.copyToClipboard(clipboardFormer))
          .hoverEvent(HoverEvent.showText(Component.text(s"Click to copy your ${event
              .getMetaType} to clipboard.")))
      )
    ))
