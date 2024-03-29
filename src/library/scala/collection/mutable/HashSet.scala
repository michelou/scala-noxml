/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */



package scala.collection
package mutable

import generic._
/*@PAR*/
import collection.parallel.mutable.ParHashSet
/*PAR@*/

/** This class implements mutable sets using a hashtable.
 *
 *  $cannotStoreNull
 *
 *  @author  Matthias Zenger
 *  @author  Martin Odersky
 *  @version 2.0, 31/12/2006
 *  @since   1
 *  @see [[http://docs.scala-lang.org/overviews/collections/concrete-mutable-collection-classes.html#hash_tables "Scala's Collection Library overview"]]
 *  section on `Hash Tables` for more information.
 *
 *  @define Coll `mutable.HashSet`
 *  @define coll mutable hash set
 *  @define thatinfo the class of the returned collection. In the standard library configuration,
 *    `That` is always `HashSet[B]` because an implicit of type `CanBuildFrom[HashSet, B, HashSet[B]]`
 *    is defined in object `HashSet`.
 *  @define bfinfo an implicit value of class `CanBuildFrom` which determines the
 *    result class `That` from the current representation type `Repr`
 *    and the new element type `B`. This is usually the `canBuildFrom` value
 *    defined in object `HashSet`.
 *  @define mayNotTerminateInf
 *  @define willNotTerminateInf
 */
@SerialVersionUID(1L)
class HashSet[A] private[collection] (contents: FlatHashTable.Contents[A])
extends AbstractSet[A]
   with Set[A]
   with GenericSetTemplate[A, HashSet]
   with SetLike[A, HashSet[A]]
   with FlatHashTable[A]
   /*@PAR*/ with CustomParallelizable[A, ParHashSet[A]] /*PAR@*/
   with Serializable
{
  initWithContents(contents)

  def this() = this(null)

  override def companion: GenericCompanion[HashSet] = HashSet

  override def size = tableSize

  def contains(elem: A): Boolean = containsEntry(elem)

  def += (elem: A): this.type = { addEntry(elem); this }

  def -= (elem: A): this.type = { removeEntry(elem); this }

  /*@PAR*/
  override def par = new ParHashSet(hashTableContents)
  /*PAR@*/

  override def add(elem: A): Boolean = addEntry(elem)

  override def remove(elem: A): Boolean = removeEntry(elem).isDefined

  override def clear() = clearTable()

  override def foreach[U](f: A =>  U) {
    var i = 0
    val len = table.length
    while (i < len) {
      val elem = table(i)
      if (elem ne null) f(elem.asInstanceOf[A])
      i += 1
    }
  }

  override def clone() = new HashSet[A] ++= this

  private def writeObject(s: java.io.ObjectOutputStream) {
    serializeTo(s)
  }

  private def readObject(in: java.io.ObjectInputStream) {
    init(in, x => x)
  }

  /** Toggles whether a size map is used to track hash map statistics.
   */
  def useSizeMap(t: Boolean) = if (t) {
    if (!isSizeMapDefined) sizeMapInitAndRebuild
  } else sizeMapDisable

}

/** $factoryInfo
 *  @define Coll mutable.HashSet
 *  @define coll mutable hash set
 */
object HashSet extends MutableSetFactory[HashSet] {
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, HashSet[A]] = setCanBuildFrom[A]
  override def empty[A]: HashSet[A] = new HashSet[A]
}

