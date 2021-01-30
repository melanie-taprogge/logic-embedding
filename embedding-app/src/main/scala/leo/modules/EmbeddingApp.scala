package leo.modules

import java.io.FileNotFoundException
import scala.io.Source

import leo.datastructures.TPTP.{Problem, AnnotatedFormula}
import leo.modules.input.TPTPParser

object EmbeddingApp {
  final val name: String = "embed"
  final val version: Double = 0.1

  private[this] var file = ""
  private[this] var outfile: Option[String] = None
  private[this] var logic: Option[String] = None
  private[this] var parameter: Set[String] = Set.empty
  private[this] var specs: Map[String, String] = Map.empty

  private[this] val embeddingTable: Map[String, Function1[Seq[AnnotatedFormula], Seq[AnnotatedFormula]]] = {
    import leo.modules.embeddings.ModalEmbedding
    Map(
      "modal" -> (x => ModalEmbedding.apply(x))
    )
  }

  final def main(args: Array[String]): Unit = {
    if (args.contains("--help")) { usage(); return }
    if (args.contains("--version")) { printVersion(); return }
    if (args.isEmpty) usage()
    else {
      var s: Option[Source] = None
      try {
        parseArgs(args.toSeq)
        s = Some(io.Source.fromFile(file))
        val parsedInput = TPTPParser.problem(s.get)
        val goalLogic = parsedInput.formulas.find(f => f.role == "logic") match {
          case Some(value) => getLogicFromSpec(value)
          case None if logic.isDefined => logic.get // TODO Also prepend logic annotated formula here
          case None => throw new NoSuchFieldException("")
        }
        val embeddedFormulas = embeddingTable(goalLogic)(parsedInput.formulas)
        val embeddedProblem = Problem(parsedInput.includes, embeddedFormulas)
        val result = generateResult(embeddedProblem)
        if (outfile.isEmpty) println(result)
        else {
          // TODO
        }
      } catch {
        case e: IllegalArgumentException => println(e.getMessage); usage()
        case e: NoSuchFieldException => println(s"Logic specification not found inside of input file and no explicit logic given via -l. Aborting.")
        case e: FileNotFoundException => println(s"Input file not found: ${e.getMessage}")
        case e: TPTPParser.TPTPParseException => println(s"Input file could not be parsed, parse error at ${e.line}:${e.offset}: ${e.getMessage}")
      } finally {
        s.foreach(_.close())
      }
    }
  }

  private[this] final def generateResult(problem: Problem): String = {
    val sb: StringBuilder = new StringBuilder()
    sb.append(s"%%% This file was generated by semantical embedding via the $name tool, version $version.\n")
    sb.append("%%%\n")
    sb.append(problem.pretty)
    sb.toString()
  }

  private[this] final def getLogicFromSpec(formula: AnnotatedFormula): String = ???

  private[this] final def printVersion(): Unit = {
    println(s"$name $version")
    }

  private[this] final def usage(): Unit = {
    println(s"usage: $name [-l <logic>] [-p <parameter>] [-s <spec>=<value>] <problem file> [<output file>]")
    println(
      """
        | <problem file> can be either a file name or '-' (without parentheses) for stdin.
        | If <output file> is specified, the result is written to <output file>, otherwise to stdout.
        |
        | Options:
        |  -l <logic>
        |     If <problem file> does not contain a logic specification statement, explicitly set
        |     the input format to <logic>.
        |     Supported <logic>s are: modal
        |
        |  -p <parameter>
        |     Pass transformation parameter <parameter> to the embedding procedure.
        |
        |  -s <spec>=<value>
        |     If <problem file> does not contain a logic specification statement, explicitly set
        |     semantics of <spec> to <value>. In this case, -l needs to be provided.
        |""".stripMargin)
  }

  private final def parseArgs(args: Seq[String]): Any = {
    var args0 = args
    while (args0.nonEmpty) {
      args0 match {
        case Seq("-l", l, rest@_*) =>
          args0 = rest
          logic = Some(l)
        case Seq("-p", p, rest@_*) =>
          args0 = rest
          parameter = parameter + p
        case Seq("-s", eq, rest@_*) =>
          args0 = rest
          eq.split("=", 2).toSeq match {
            case Seq(l, r) => specs = specs + (l -> r)
            case _ => throw new IllegalArgumentException(s"Malformed argument to -s option: '$eq'")
          }
          specs
        case Seq(f) =>
          args0 = Seq.empty
          file = f
        case Seq(f, o) =>
          args0 = Seq.empty
          file = f
          outfile = Some(o)
        case _ => throw new IllegalArgumentException("Unrecognized arguments.")
      }
    }

  }
}
