package org.fwb.file.xml;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import com.google.common.collect.ImmutableList;

import org.fwb.file.FileProperty.FileField;

public class FileSystemXMLReader extends XMLFilterImpl {
	static final String
		NS = XMLConstants.NULL_NS_URI,
		FILE = "file",
		DIR = "dir";
	
	static final List<FileField> DEFAULT_FIELDS = ImmutableList.of(
		FileField.name, FileField.lengthString, FileField.lastModifiedString);
	
	final List<FileField> FIELDS;
	
	public FileSystemXMLReader() {
		this(DEFAULT_FIELDS);
	}
	public FileSystemXMLReader(List<FileField> fields) {
		FIELDS = fields;
	}
	
	@Override
	public void parse(InputSource s) throws IOException, SAXException {
		File file = null;
		try {
			file = new File(new URI(s.getSystemId()));	// URISyntaxException, IllegalArgumentException
		} catch (Exception e) { }
		
		if (file == null)
			super.parse(s);
		else {
			startDocument();
				parse(file);
			endDocument();
		}
	}
	
	/**
	 * the payload parsing method, recursive.
	 * @throws IOException this implementation does not throw IOException, but subclasses are permitted
	 */
	void parse(File f) throws SAXException, IOException {
		boolean dir = f.isDirectory();
		String tag = dir ? DIR : FILE;
		startElement(NS, tag, tag, new FileAttributes(f, FIELDS));
			if (dir)
				for (File c : f.listFiles())
					parse(c);	// recurse
		endElement(NS, tag, tag);
	}
	
	public static InputSource getFileInputSource(File f) {
		return new InputSource(f.toURI().toString());
	}
	
	/** @return a source which parses with this class */
	public static SAXSource getFileSource(File f) {
		return new SAXSource(
				new FileSystemXMLReader(),
				getFileInputSource(f));
	}
}