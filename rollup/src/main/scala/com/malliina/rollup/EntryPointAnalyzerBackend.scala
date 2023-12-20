package com.malliina.rollup

import org.scalajs.linker.interface.*
import org.scalajs.linker.standard.*
import org.scalajs.logging.Logger

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

final class EntryPointAnalyzerBackend(linkerConfig: StandardConfig, entryPointOutputFile: Path)
  extends LinkerBackend {
  private val standard = StandardLinkerBackend(linkerConfig)

  val coreSpec: CoreSpec = standard.coreSpec
  val symbolRequirements: SymbolRequirement = standard.symbolRequirements

  def injectedIRFiles: Seq[IRFile] = standard.injectedIRFiles

  def emit(moduleSet: ModuleSet, output: OutputDirectory, logger: Logger)(implicit
    ec: ExecutionContext
  ): Future[Report] = {
    val modules = moduleSet.modules.flatMap(_.externalDependencies).toSet
    Files.write(entryPointOutputFile, modules.asJava, StandardCharsets.UTF_8)
    standard.emit(moduleSet, output, logger)
  }
}
