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
import parallel.mutable.ParSeq
/*PAR@*/

/** A template trait for mutable sequences of type `mutable.Seq[A]`.
 *  @tparam A    the type of the elements of the set
 *  @tparam This the type of the set itself.
 *
 */
trait SeqLike[A, +This <: SeqLike[A, This] with Seq[A]]
  extends scala.collection.SeqLike[A, This]
     with Cloneable[This]
     /*@PAR*/ with Parallelizable[A, ParSeq[A]] /*PAR@*/
{
  self =>

  /*@PAR*/
  protected[this] override def parCombiner = ParSeq.newCombiner[A]
  /*PAR@*/

  /** Replaces element at given index with a new value.
   *
   *  @param idx      the index of the element to replace.
   *  @param elem     the new value.
   *  @throws   IndexOutOfBoundsException if the index is not valid.
   */
  def update(idx: Int, elem: A)

  /** Applies a transformation function to all values contained in this sequence.
   *  The transformation function produces new values from existing elements.
   *
   * @param f  the transformation to apply
   * @return   the sequence itself.
   */
  def transform(f: A => A): this.type = {
    var i = 0
    this foreach { el =>
      this(i) = f(el)
      i += 1
    }
    this
  }
}
