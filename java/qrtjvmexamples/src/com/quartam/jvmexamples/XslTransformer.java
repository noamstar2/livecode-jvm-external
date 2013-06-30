/* Copyright (C) 2008-2013 Quartam Software / Jan Schenkel.
 
 This file is part of the [qrtjvm] project, also known as
 Quartam JavaVirtualMachine External for LiveCode.
 
 [qrtjvm] is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License v3 as published by the Free
 Software Foundation.
 
 [qrtjvm] is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.
 
 You should have received a copy of the GNU General Public License
 along with [qrtjvm].  If not see <http://www.gnu.org/licenses/>.  */

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

@ExternalPackage(author="Jan Schenkel", date="20090814")
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
