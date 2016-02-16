package org.fwb.file.xml;

import java.io.File;
import java.util.List;

import javax.xml.XMLConstants;

import org.xml.sax.Attributes;

import org.fwb.file.FileProperty.FileField;
import org.fwb.xml.sax.SaxUtil;

/**
 * N.B. the code-duplication herein is deliberate:
 * a minor sacrifice of elegance for minor (linear by content size) performance gain.
 * 
 * @deprecated TODO also for performance, I do not bounds-check (in either index or name case).
 * this is not actually according to spec, which says I'm required to return null/-1.
 * for instance, the constructor is intended to receive a SUBset of FileFields, yet getValue(localName) respects *any* existing FileField.
 * therefore, this class should be replaced with an implementation of List or Map,
 * then use ListAttributes (or its Map facility)
 */
final class FileAttributes implements Attributes {
	final File F;
	final List<? extends FileField> FIELDS;
	
	public FileAttributes(File f, List<? extends FileField> fields) {
		F = f;
		FIELDS = fields;
	}
	
	@Override
	public int getLength() {
		return FIELDS.size();
	}
	
	@Override
	public String getURI(int i) {
		return XMLConstants.NULL_NS_URI;
	}
	@Override
	public String getLocalName(int i) {
		return FIELDS.get(i).name();
	}
	@Override
	public String getQName(int i) {
		return FIELDS.get(i).name();
	}
	
	@Override
	public int getIndex(String uri, String localName) {
		return FIELDS.indexOf(FileField.valueOf(localName));
	}
	@Override
	public int getIndex(String localName) {
		return FIELDS.indexOf(FileField.valueOf(localName));
	}
	
	@Override
	public String getValue(int i) {
		return FIELDS.get(i).apply(F).toString();
	}
	@Override
	public String getValue(String uri, String localName) {
		return FileField.valueOf(localName).apply(F).toString();
	}
	@Override
	public String getValue(String localName) {
		return FileField.valueOf(localName).apply(F).toString();
	}
	
	@Override
	public String getType(int i) {
		return SaxUtil.CDATA;
	}
	@Override
	public String getType(String uri, String localName) {
		return SaxUtil.CDATA;
	}
	@Override
	public String getType(String localName) {
		return SaxUtil.CDATA;
	}
}