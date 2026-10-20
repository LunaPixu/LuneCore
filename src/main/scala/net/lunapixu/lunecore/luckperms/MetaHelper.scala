package net.lunapixu.lunecore.luckperms

import net.luckperms.api.node.types.SuffixNode
import net.luckperms.api.node.types.PrefixNode
import net.luckperms.api.node.types.PrefixNode.Builder as PrefixBuilder
import net.luckperms.api.node.types.SuffixNode.Builder as SuffixBuilder

object MetaHelper:
  type Tuple = (value: String, weight: Integer)
  type Node = SuffixNode | PrefixNode
  /* This type does not play nice AT ALL when I try to reduce it via MetaNode.
  It's as if the compiler effs up or can't tell the difference between the node types.
  So here I am having to manually type check at every corner and pray that I don't screw up. */
  type NodeBuilder = SuffixBuilder | PrefixBuilder

  // These would be tuples but my IDE breaks big time around those
  case class Wrapper(rawString: String, node: Node)
  case class OptionalWrapper(rawString: Option[String], node: Option[Node])
