/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2011, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.runtime

import scala.collection.{ Seq, IndexedSeq, TraversableView, AbstractIterator }
import scala.collection.mutable.WrappedArray
import scala.collection.immutable.{ StringLike, NumericRange, List, Stream, Nil, :: }
import scala.collection.generic.{ Sorted }
import scala.util.control.ControlThrowable
/*@XML
import scala.xml.{ Node, MetaData }
XML@*/

import java.lang.Double.doubleToLongBits
import java.lang.reflect.{ Modifier, Method => JMethod }

/** The object ScalaRunTime provides support methods required by
 *  the scala runtime.  All these methods should be considered
 *  outside the API and subject to change or removal without notice.
 */
object ScalaRunTime {
  def isArray(x: AnyRef): Boolean = isArray(x, 1)
  def isArray(x: Any, atLevel: Int): Boolean =
    x != null && isArrayClass(x.getClass, atLevel)

  private def isArrayClass(clazz: Class[_], atLevel: Int): Boolean =
    clazz.isArray && (atLevel == 1 || isArrayClass(clazz.getComponentType, atLevel - 1))

  def isValueClass(clazz: Class[_]) = clazz.isPrimitive()
  def isTuple(x: Any) = x != null && tupleNames(x.getClass.getName)
  def isAnyVal(x: Any) = x match {
    case _: Byte | _: Short | _: Char | _: Int | _: Long | _: Float | _: Double | _: Boolean | _: Unit => true
    case _                                                                                             => false
  }
  // Avoiding boxing which messes up the specialized tests.  Don't ask.
  private val tupleNames = {
    var i = 22
    var names: List[String] = Nil
    while (i >= 1) {
      names ::= ("scala.Tuple" + String.valueOf(i))
      i -= 1
    }
    names.toSet
  }

  /** Return the class object representing an array with element class `clazz`.
   */
  def arrayClass(clazz: Class[_]): Class[_] = {
    // newInstance throws an exception if the erasure is Void.TYPE. see SI-5680
    if (clazz == java.lang.Void.TYPE) classOf[Array[Unit]]
    else java.lang.reflect.Array.newInstance(clazz, 0).getClass
  }

  /** Return the class object representing elements in arrays described by a given schematic.
   */
  def arrayElementClass(schematic: Any): Class[_] = schematic match {
    case cls: Class[_] => cls.getComponentType
    case tag: ClassTag[_] => tag.erasure
    case tag: ArrayTag[_] => tag.newArray(0).getClass.getComponentType
    case _ => throw new UnsupportedOperationException("unsupported schematic %s (%s)".format(schematic, if (schematic == null) "null" else schematic.getClass))
  }

  /** Return the class object representing an unboxed value type,
   *  e.g. classOf[int], not classOf[java.lang.Integer].  The compiler
   *  rewrites expressions like 5.getClass to come here.
   */
  def anyValClass[T <: AnyVal : ClassTag](value: T): Class[T] =
    classTag[T].erasure.asInstanceOf[Class[T]]

  /** Retrieve generic array element */
  def array_apply(xs: AnyRef, idx: Int): Any = xs match {
    case x: Array[AnyRef]  => x(idx).asInstanceOf[Any]
    case x: Array[Int]     => x(idx).asInstanceOf[Any]
    case x: Array[Double]  => x(idx).asInstanceOf[Any]
    case x: Array[Long]    => x(idx).asInstanceOf[Any]
    case x: Array[Float]   => x(idx).asInstanceOf[Any]
    case x: Array[Char]    => x(idx).asInstanceOf[Any]
    case x: Array[Byte]    => x(idx).asInstanceOf[Any]
    case x: Array[Short]   => x(idx).asInstanceOf[Any]
    case x: Array[Boolean] => x(idx).asInstanceOf[Any]
    case x: Array[Unit]    => x(idx).asInstanceOf[Any]
    case null => throw new NullPointerException
  }

  /** update generic array element */
  def array_update(xs: AnyRef, idx: Int, value: Any): Unit = xs match {
    case x: Array[AnyRef]  => x(idx) = value.asInstanceOf[AnyRef]
    case x: Array[Int]     => x(idx) = value.asInstanceOf[Int]
    case x: Array[Double]  => x(idx) = value.asInstanceOf[Double]
    case x: Array[Long]    => x(idx) = value.asInstanceOf[Long]
    case x: Array[Float]   => x(idx) = value.asInstanceOf[Float]
    case x: Array[Char]    => x(idx) = value.asInstanceOf[Char]
    case x: Array[Byte]    => x(idx) = value.asInstanceOf[Byte]
    case x: Array[Short]   => x(idx) = value.asInstanceOf[Short]
    case x: Array[Boolean] => x(idx) = value.asInstanceOf[Boolean]
    case x: Array[Unit]    => x(idx) = value.asInstanceOf[Unit]
    case null => throw new NullPointerException
  }

  /** Get generic array length */
  def array_length(xs: AnyRef): Int = xs match {
    case x: Array[AnyRef]  => x.length
    case x: Array[Int]     => x.length
    case x: Array[Double]  => x.length
    case x: Array[Long]    => x.length
    case x: Array[Float]   => x.length
    case x: Array[Char]    => x.length
    case x: Array[Byte]    => x.length
    case x: Array[Short]   => x.length
    case x: Array[Boolean] => x.length
    case x: Array[Unit]    => x.length
    case null => throw new NullPointerException
  }

  def array_clone(xs: AnyRef): AnyRef = xs match {
    case x: Array[AnyRef]  => ArrayRuntime.cloneArray(x)
    case x: Array[Int]     => ArrayRuntime.cloneArray(x)
    case x: Array[Double]  => ArrayRuntime.cloneArray(x)
    case x: Array[Long]    => ArrayRuntime.cloneArray(x)
    case x: Array[Float]   => ArrayRuntime.cloneArray(x)
    case x: Array[Char]    => ArrayRuntime.cloneArray(x)
    case x: Array[Byte]    => ArrayRuntime.cloneArray(x)
    case x: Array[Short]   => ArrayRuntime.cloneArray(x)
    case x: Array[Boolean] => ArrayRuntime.cloneArray(x)
    case x: Array[Unit]    => x
    case null => throw new NullPointerException
  }

  /** Convert an array to an object array.
   *  Needed to deal with vararg arguments of primitive types that are passed
   *  to a generic Java vararg parameter T ...
   */
  def toObjectArray(src: AnyRef): Array[Object] = src match {
    case x: Array[AnyRef] => x
    case _ =>
      val length = array_length(src)
      val dest = new Array[Object](length)
      for (i <- 0 until length)
        array_update(dest, i, array_apply(src, i))
      dest
  }

  def toArray[T](xs: collection.Seq[T]) = {
    val arr = new Array[AnyRef](xs.length)
    var i = 0
    for (x <- xs) {
      arr(i) = x.asInstanceOf[AnyRef]
      i += 1
    }
    arr
  }

  // Java bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4071957
  // More background at ticket #2318.
  def ensureAccessible(m: JMethod): JMethod = {
    if (!m.isAccessible) {
      try m setAccessible true
      catch { case _: SecurityException => () }
    }
    m
  }

  def checkInitialized[T <: AnyRef](x: T): T =
    if (x == null) throw new UninitializedError else x

  abstract class Try[+A] {
    def Catch[B >: A](handler: PartialFunction[Throwable, B]): B
    def Finally(fin: => Unit): A
  }

  def Try[A](block: => A): Try[A] = new Try[A] with Runnable {
    private var result: A = _
    private var exception: Throwable =
      try   { run() ; null }
      catch {
        case e: ControlThrowable  => throw e  // don't catch non-local returns etc
        case e: Throwable         => e
      }

    def run() { result = block }

    def Catch[B >: A](handler: PartialFunction[Throwable, B]): B =
      if (exception == null) result
      else if (handler isDefinedAt exception) handler(exception)
      else throw exception

    def Finally(fin: => Unit): A = {
      fin

      if (exception == null) result
      else throw exception
    }
  }

  def _toString(x: Product): String =
    x.productIterator.mkString(x.productPrefix + "(", ",", ")")

  def _hashCode(x: Product): Int = scala.util.MurmurHash3.productHash(x)

  /** A helper for case classes. */
  def typedProductIterator[T](x: Product): Iterator[T] = {
    new AbstractIterator[T] {
      private var c: Int = 0
      private val cmax = x.productArity
      def hasNext = c < cmax
      def next() = {
        val result = x.productElement(c)
        c += 1
        result.asInstanceOf[T]
      }
    }
  }

  /** Fast path equality method for inlining; used when -optimise is set.
   */
  @inline def inlinedEquals(x: Object, y: Object): Boolean =
    if (x eq y) true
    else if (x eq null) false
    else if (x.isInstanceOf[java.lang.Number]) BoxesRunTime.equalsNumObject(x.asInstanceOf[java.lang.Number], y)
    else if (x.isInstanceOf[java.lang.Character]) BoxesRunTime.equalsCharObject(x.asInstanceOf[java.lang.Character], y)
    else x.equals(y)

  def _equals(x: Product, y: Any): Boolean = y match {
    case y: Product if x.productArity == y.productArity => x.productIterator sameElements y.productIterator
    case _                                              => false
  }

  // hashcode -----------------------------------------------------------
  //
  // Note that these are the implementations called by ##, so they
  // must not call ## themselves.

  @inline def hash(x: Any): Int =
    if (x == null) 0
    else if (x.isInstanceOf[java.lang.Number]) BoxesRunTime.hashFromNumber(x.asInstanceOf[java.lang.Number])
    else x.hashCode

  @inline def hash(dv: Double): Int = {
    val iv = dv.toInt
    if (iv == dv) return iv

    val lv = dv.toLong
    if (lv == dv) return lv.hashCode

    val fv = dv.toFloat
    if (fv == dv) fv.hashCode else dv.hashCode
  }
  @inline def hash(fv: Float): Int = {
    val iv = fv.toInt
    if (iv == fv) return iv

    val lv = fv.toLong
    if (lv == fv) return hash(lv)
    else fv.hashCode
  }
  @inline def hash(lv: Long): Int = {
    val low = lv.toInt
    val lowSign = low >>> 31
    val high = (lv >>> 32).toInt
    low ^ (high + lowSign)
  }
  @inline def hash(x: Number): Int  = runtime.BoxesRunTime.hashFromNumber(x)

  // The remaining overloads are here for completeness, but the compiler
  // inlines these definitions directly so they're not generally used.
  @inline def hash(x: Int): Int = x
  @inline def hash(x: Short): Int = x.toInt
  @inline def hash(x: Byte): Int = x.toInt
  @inline def hash(x: Char): Int = x.toInt
  @inline def hash(x: Boolean): Int = if (x) true.hashCode else false.hashCode
  @inline def hash(x: Unit): Int = 0

  /** A helper method for constructing case class equality methods,
   *  because existential types get in the way of a clean outcome and
   *  it's performing a series of Any/Any equals comparisons anyway.
   *  See ticket #2867 for specifics.
   */
  def sameElements(xs1: collection.Seq[Any], xs2: collection.Seq[Any]) = xs1 sameElements xs2

  /** Given any Scala value, convert it to a String.
   *
   * The primary motivation for this method is to provide a means for
   * correctly obtaining a String representation of a value, while
   * avoiding the pitfalls of naïvely calling toString on said value.
   * In particular, it addresses the fact that (a) toString cannot be
   * called on null and (b) depending on the apparent type of an
   * array, toString may or may not print it in a human-readable form.
   *
   * @param   arg   the value to stringify
   * @return        a string representation of arg.
   */
  def stringOf(arg: Any): String = stringOf(arg, scala.Int.MaxValue)
  def stringOf(arg: Any, maxElements: Int): String = {
    def packageOf(x: AnyRef) = x.getClass.getPackage match {
      case null   => ""
      case p      => p.getName
    }
    def isScalaClass(x: AnyRef)         = packageOf(x) startsWith "scala."
    def isScalaCompilerClass(x: AnyRef) = packageOf(x) startsWith "scala.tools.nsc."

    // When doing our own iteration is dangerous
    def useOwnToString(x: Any) = x match {
      /*@XML
      // Node extends NodeSeq extends Seq[Node] and MetaData extends Iterable[MetaData]
      case _: Node | _: MetaData => true
      XML@*/
      // Range/NumericRange have a custom toString to avoid walking a gazillion elements
      case _: Range | _: NumericRange[_] => true
      // Sorted collections to the wrong thing (for us) on iteration - ticket #3493
      case _: Sorted[_, _]  => true
      // StringBuilder(a, b, c) and similar not so attractive
      case _: StringLike[_] => true
      // Don't want to evaluate any elements in a view
      case _: TraversableView[_, _] => true
      // Don't want to a) traverse infinity or b) be overly helpful with peoples' custom
      // collections which may have useful toString methods - ticket #3710
      // or c) print AbstractFiles which are somehow also Iterable[AbstractFile]s.
      case x: Traversable[_] => !x.hasDefiniteSize || !isScalaClass(x) || isScalaCompilerClass(x)
      // Otherwise, nothing could possibly go wrong
      case _ => false
    }

    // A variation on inner for maps so they print -> instead of bare tuples
    def mapInner(arg: Any): String = arg match {
      case (k, v)   => inner(k) + " -> " + inner(v)
      case _        => inner(arg)
    }

    // Special casing Unit arrays, the value class which uses a reference array type.
    def arrayToString(x: AnyRef) = {
      if (x.getClass.getComponentType == classOf[BoxedUnit])
        0 until (array_length(x) min maxElements) map (_ => "()") mkString ("Array(", ", ", ")")
      else
        WrappedArray make x take maxElements map inner mkString ("Array(", ", ", ")")
    }

    // The recursively applied attempt to prettify Array printing.
    // Note that iterator is used if possible and foreach is used as a
    // last resort, because the parallel collections "foreach" in a
    // random order even on sequences.
    def inner(arg: Any): String = arg match {
      case null                         => "null"
      case ""                           => "\"\""
      case x: String                    => if (x.head.isWhitespace || x.last.isWhitespace) "\"" + x + "\"" else x
      case x if useOwnToString(x)       => x.toString
      case x: AnyRef if isArray(x)      => arrayToString(x)
      case x: collection.Map[_, _]      => x.iterator take maxElements map mapInner mkString (x.stringPrefix + "(", ", ", ")")
      case x: Iterable[_]               => x.iterator take maxElements map inner mkString (x.stringPrefix + "(", ", ", ")")
      case x: Traversable[_]            => x take maxElements map inner mkString (x.stringPrefix + "(", ", ", ")")
      case x: Product1[_] if isTuple(x) => "(" + inner(x._1) + ",)" // that special trailing comma
      case x: Product if isTuple(x)     => x.productIterator map inner mkString ("(", ",", ")")
      case x                            => x.toString
    }

    // The try/catch is defense against iterables which aren't actually designed
    // to be iterated, such as some scala.tools.nsc.io.AbstractFile derived classes.
    try inner(arg)
    catch {
      case _: StackOverflowError | _: UnsupportedOperationException | _: AssertionError => "" + arg
    }
  }

  /** stringOf formatted for use in a repl result. */
  def replStringOf(arg: Any, maxElements: Int): String = {
    val s  = stringOf(arg, maxElements)
    val nl = if (s contains "\n") "\n" else ""

    nl + s + "\n"
  }
  private[scala] def checkZip(what: String, coll1: TraversableOnce[_], coll2: TraversableOnce[_]) {
    if (sys.props contains "scala.debug.zip") {
      val xs = coll1.toIndexedSeq
      val ys = coll2.toIndexedSeq
      if (xs.length != ys.length) {
        Console.err.println(
          "Mismatched zip in " + what + ":\n" +
          "  this: " + xs.mkString(", ") + "\n" +
          "  that: " + ys.mkString(", ")
        )
        (new Exception).getStackTrace.drop(2).take(10).foreach(println)
      }
    }
  }
}
