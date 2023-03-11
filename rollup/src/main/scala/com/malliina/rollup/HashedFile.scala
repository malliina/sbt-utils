package com.malliina.rollup

import com.malliina.storage.StorageLong

import java.nio.file.{Files, Path}

case class HashedFile(path: String, hashedPath: String, originalFile: Path, hashedFile: Path) {
  def size = Files.size(originalFile).bytes
}

object HashedFile {
  def from(original: Path, hashed: Path, root: Path) = HashedFile(
    root.relativize(original).toString.replace('\\', '/'),
    root.relativize(hashed).toString.replace('\\', '/'),
    original,
    hashed
  )
}
