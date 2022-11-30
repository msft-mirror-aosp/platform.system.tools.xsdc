/*
 * Copyright (C) 2022, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.xsdc

import com.android.xsdc.cpp.CppCodeGenerator
import java.io.ByteArrayInputStream
import java.util.TreeMap
import javax.xml.parsers.SAXParserFactory
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TestCppCodeGenerator {

  val classSchema =
    schema(
      """
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
          <xs:element name="class" type="xs:string" />
        </xs:schema>
        """
    )

  @Test
  fun parseSchema() {
    assertEquals(classSchema.elementMap.keys, setOf("class"))
  }

  @Test
  fun printWithoutEnumOutput() {
    val files = TreeMap<String, StringBuffer>()
    val fs = FileSystem(files)
    val gen =
      CppCodeGenerator(
        classSchema,
        "com.abc",
        /*writer=*/ true,
        CppCodeGenerator.GENERATE_PARSER,
        /*booleanGetter=*/ true,
        /*useTinyXml=*/ false
      )
    gen.print(fs)

    assertEquals(files.keys, setOf("com_abc.cpp", "include/com_abc.h"))
  }

  @Test
  fun printWithEnumOutput() {
    val files = TreeMap<String, StringBuffer>()
    val fs = FileSystem(files)
    val gen =
      CppCodeGenerator(
        classSchema,
        "com.abc",
        /*writer=*/ true,
        CppCodeGenerator.GENERATE_PARSER or CppCodeGenerator.GENERATE_ENUMS,
        /*booleanGetter=*/ true,
        /*useTinyXml=*/ false
      )
    gen.print(fs)

    assertEquals(
      files.keys,
      setOf("com_abc.cpp", "include/com_abc.h", "com_abc_enums.cpp", "include/com_abc_enums.h")
    )
  }

  fun schema(contents: String): XmlSchema {
    val charset = Charsets.UTF_8
    val bytes = contents.toByteArray(charset)
    val factory = SAXParserFactory.newInstance()
    factory.setNamespaceAware(true)
    val parser = factory.newSAXParser()
    val xsdHandler = XsdHandler()
    parser.parse(ByteArrayInputStream(bytes), xsdHandler)
    return xsdHandler.getSchema()
  }
}
