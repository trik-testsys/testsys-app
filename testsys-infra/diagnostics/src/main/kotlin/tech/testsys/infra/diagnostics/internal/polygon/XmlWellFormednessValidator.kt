package tech.testsys.infra.diagnostics.internal.polygon

import org.xml.sax.InputSource
import org.xml.sax.SAXParseException
import org.xml.sax.helpers.DefaultHandler
import tech.testsys.infra.diagnostics.internal.InternalDiagnosticsApi
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

@InternalDiagnosticsApi
internal class XmlWellFormednessValidator {
    fun error(text: String): String? {
        val factory = SAXParserFactory.newInstance()
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        val reader = factory.newSAXParser().xmlReader
        reader.errorHandler = object : DefaultHandler() {
            override fun error(exception: SAXParseException): Nothing = throw exception
            override fun fatalError(exception: SAXParseException): Nothing = throw exception
        }
        return try {
            reader.parse(InputSource(StringReader(text)))
            null
        } catch (exception: SAXParseException) {
            exception.message ?: "Malformed XML"
        }
    }
}
