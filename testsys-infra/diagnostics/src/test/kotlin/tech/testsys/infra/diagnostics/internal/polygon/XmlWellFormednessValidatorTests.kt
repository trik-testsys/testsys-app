package tech.testsys.infra.diagnostics.internal.polygon

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi

@OptIn(InternalDiagnosticsApi::class)
class XmlWellFormednessValidatorTests {
    @Test
    fun `should reject a mismatched closing tag that a recovering parser could accept`() {
        assertNotNull(XmlWellFormednessValidator().error("<root><world></root>"))
    }

    @Test
    fun `should preserve comments and XML declarations in well formed documents`() {
        assertNull(XmlWellFormednessValidator().error("<?xml version='1.0'?><root><!--comment--><world/></root>"))
    }

    @Test
    fun `should reject external entity declarations without accessing their source`() {
        val xml = "<!DOCTYPE root [<!ENTITY content SYSTEM 'file:///missing'>]><root>&content;</root>"

        assertNotNull(XmlWellFormednessValidator().error(xml))
    }
}
