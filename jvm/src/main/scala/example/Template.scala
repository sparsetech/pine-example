package example

import pine._

import scala.collection.mutable
import scala.io.Source

import java.io.File
import java.nio.file._

import scala.collection.JavaConverters._

object Template {
  def watchFiles(directory: File)(onModify: String => Unit): Unit = new Thread {
    val watcher = FileSystems.getDefault.newWatchService
    directory.toPath.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY)

    start()  // Start thread

    override def run(): Unit = {
      @scala.annotation.tailrec
      def f(): Unit = {
        val key = watcher.take

        key.pollEvents.asScala.foreach { event =>
          val fileName = event.context()

          event.kind match {
            case StandardWatchEventKinds.OVERFLOW     => Thread.`yield`()
            case StandardWatchEventKinds.ENTRY_MODIFY => onModify(fileName.toString)
          }
        }

        if (key.reset) f()
      }

      f()
    }
  }

  val cache = mutable.HashMap.empty[String, Tag[_]]
  val path  = new File("assets/html")

  watchFiles(path) { p =>
    val template = p.stripSuffix(".html")
    if (cache.contains(template)) {
      println(s"Template `$template` changed")
      cache -= template
    }
  }

  def fetch(template: String): String =
    Source.fromFile(new File(path, s"$template.html")).mkString

  def get(template: String): Tag[_] =
    cache.getOrElseUpdate(template,
      HtmlParser.fromString(fetch(template)).asInstanceOf[Tag[_]])

  def all(): Map[String, String] =
    path
      .listFiles()
      .map(_.toPath.getFileName.toString)
      .filter(_.endsWith(".html"))
      .map(_.stripSuffix(".html"))
      .map(template => template -> fetch(template))
      .toMap
}
