package net.lunapixu.lunecore.luckperms

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.Component
import net.luckperms.api.cacheddata.Result
import net.luckperms.api.model.user.User
import net.luckperms.api.node.types.{ChatMetaNode, PrefixNode, SuffixNode}
import net.luckperms.api.node.types.PrefixNode.Builder as PrefixBuilder
import net.luckperms.api.node.types.SuffixNode.Builder as SuffixBuilder
import net.luckperms.api.node.Node
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag
import scala.util.Try

abstract class editsMeta(metaType: String):
  protected type MetaTuple = (value: String, weight: Integer)
  protected type MetaNode = SuffixNode | PrefixNode
  /* This type does not play nice AT ALL when I try to reduce it via MetaNode.
  It's as if the compiler effs up or can't tell the difference between the node types.
  So here I am having to manually type check at every corner and pray that I don't screw up. */
  protected type MetaNodeBuilder = SuffixBuilder | PrefixBuilder

  private val configPath = s"commands.set-${metaType}"
  def getConfigPath = configPath
  def getMetaType = metaType

  protected def nodeEqualsMeta[T <: MetaNode: ClassTag](node: Node, metaTuple: MetaTuple): Boolean =
    node match
      case metaNode: T => metaNode.getMetaValue == metaTuple.value
      case _           => false

  protected def updateUserMeta[T <: MetaNode: ClassTag](
    user: User,
    metaStr: String,
    nodeBuilder: MetaNodeBuilder,
    configPath: String
  )(using config: FileConfiguration): Unit =
    val userMetadata = user.getCachedData.getMetaData

    val metaNodes = nodeBuilder match
      case p: PrefixBuilder => userMetadata.getPrefixes
      case s: SuffixBuilder => userMetadata.getSuffixes
    val highestWeight = metaNodes.entrySet.asScala.map { entry => (entry.getValue, entry.getKey) }
      .foldLeft(1) { (acc, p: MetaTuple) => acc max p.weight }

    val metaPath = nodeBuilder match
      case p: PrefixBuilder => "prefix-template"
      case s: SuffixBuilder => "suffix-template"
    val rank = userMetadata.getPrimaryGroup
    val metaTemplate = Option(config.getString(s"${configPath}.${metaPath}.${rank}"))
      .getOrElse(config.getString(s"${configPath}.${metaPath}.default", "%s "))
    val formattedMeta = String.format(metaTemplate, metaStr)
    val metaNode: Node = nodeBuilder match
      case p: PrefixBuilder => p.prefix(formattedMeta).priority(highestWeight).build
      case s: SuffixBuilder => s.suffix(formattedMeta).priority(highestWeight).build

    val nodeQuery: Result[String, PrefixNode] | Result[String, SuffixNode] = nodeBuilder match
      case p: PrefixBuilder => userMetadata.queryPrefix
      case s: SuffixBuilder => userMetadata.querySuffix
    Try(nodeQuery.node).fold(e => (), (n: Node) => user.data.remove(n))
    user.data.add(metaNode)

  def makeUpdatedMetaComponent(targetPlayer: Player): Component = targetPlayer.displayName
    .append(Component.text(s" has had their ${getMetaType} updated!", NamedTextColor.GOLD))
