package com.quartam.jvmexamples;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.quartam.external.ExternalException;
import com.quartam.external.ExternalFunction;
import com.quartam.external.ExternalPackage;

@ExternalPackage(author="Jan Schenkel", date="200900814")
public class XslTransformer {

	public XslTransformer() {
		super();
	}
	
	@ExternalFunction
	public String xslTransformXmlUsingXsl(String[] params) throws ExternalException {
		String xmlString = params[0];
		String xslString = params[1];
		StringWriter resWriter = new StringWriter();
		StringReader xmlReader = new StringReader(xmlString);
		StringReader xslReader = new StringReader(xslString);
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer(new StreamSource(xslReader));
			transformer.transform(new StreamSource(xmlReader), new StreamResult(resWriter));
		} catch (Exception e) {
			throw new ExternalException(e.getMessage());
		} finally {
			xmlReader.close();
			xslReader.close();
		}
		return resWriter.toString();
	}
}
