package com.malliina.sbt.filetree

class IdentifierTests extends munit.FunSuite with ScalaIdentifiers {
  test("camelCase") {
    assertEquals(camelCase("a-b"), "aB")
    assertEquals(camelCase("raar_"), "raar_")
  }

  test("negative camelCase") {
    assertEquals(camelCase("blaaBlaa"), "blaaBlaa")
  }

  test("sanitize") {
    assertEquals(sanitize("class"), "`class`")
  }

  test("negative sanitize") {
    assertEquals(sanitize("huuhaa"), "huuhaa")
  }

  test("worst case scenario") {
    assertEquals(legalName("for_some"), "for_some")
  }

  test("legality") {
    assertEquals(legalName("app-5.newest"), "app_5_newest")
  }
}
