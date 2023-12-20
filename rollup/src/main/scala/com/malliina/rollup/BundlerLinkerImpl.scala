package com.malliina.rollup

import org.scalajs.linker.*
import org.scalajs.linker.interface.*
import org.scalajs.linker.standard.*
import org.scalajs.sbtplugin.LinkerImpl

import java.nio.file.Path

object BundlerLinkerImpl {
  def linker(config: StandardConfig, entryPointOutputFile: Path): Linker = {
    val frontend = StandardLinkerFrontend(config)
    val backend = new EntryPointAnalyzerBackend(config, entryPointOutputFile)
    StandardLinkerImpl(frontend, backend)
  }

  def clearableLinker(config: StandardConfig, entryPointOutputFile: Path): ClearableLinker =
    ClearableLinker(() => linker(config, entryPointOutputFile), config.batchMode)
}

class ForwardingLinker(base: LinkerImpl.Reflect) extends LinkerImpl.Forwarding(base) {
  def bundlerLinker(config: StandardConfig, entryPointOutputFile: Path): ClearableLinker = {
    BundlerLinkerImpl.clearableLinker(config, entryPointOutputFile)
  }
}
