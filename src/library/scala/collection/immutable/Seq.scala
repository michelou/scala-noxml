/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */



package scala.collection
package immutable

import generic._
import mutable.Builder
/*@PAR*/
import parallel.immutable.ParSeq
/*PAR@*/

/** A subtrait of `collection.Seq` which represents sequences
 *  that are guaranteed immutable.
 *
 *  $seqInfo
 *  @define Coll `immutable.Seq`
 *  @define coll immutable sequence
 */
trait Seq[+A] extends Iterable[A]
//                      with GenSeq[A]
                      with scala.collection.Seq[A]
                      with GenericTraversableTemplate[A, Seq]
                      with SeqLike[A, Seq[A]]
                      /*@PAR*/ with Parallelizable[A, ParSeq[A]] /*PAR@*/
{
  override def companion: GenericCompanion[Seq] = Seq
  override def toSeq: Seq[A] = this
  override def seq: Seq[A] = this
  /*@PAR*/
  protected[this] override def parCombiner = ParSeq.newCombiner[A] // if `immutable.SeqLike` gets introduced, please move this there!
  /*PAR@*/
}

/** $factoryInfo
 *  @define Coll `immutable.Seq`
 *  @define coll immutable sequence
 */
object Seq extends SeqFactory[Seq] {
  /** genericCanBuildFromInfo */
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, Seq[A]] = ReusableCBF.asInstanceOf[GenericCanBuildFrom[A]]
  def newBuilder[A]: Builder[A, Seq[A]] = new mutable.ListBuffer
}
