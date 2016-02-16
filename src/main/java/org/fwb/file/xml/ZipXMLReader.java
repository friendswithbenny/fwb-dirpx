package org.fwb.file.xml;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.transform.sax.SAXSource;

import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableSet;

import org.fwb.file.FileUtil;
import org.fwb.file.zip.ZipDirectory;
import org.fwb.file.FileProperty.FileField;

public class ZipXMLReader extends FileSystemXMLReader {
	
	static final Set<String> DEFAULT_UNZIP_EXTENSIONS =
			ImmutableSet.of("zip", "jar", "rar");
	
	static final String TAG_ZIP = "zip";
	
	final Collection<String> UNZIP_EXTENSIONS;
	
	public ZipXMLReader() {
		this(DEFAULT_FIELDS, DEFAULT_UNZIP_EXTENSIONS);
	}
	
	public ZipXMLReader(List<FileField> fields, Collection<String> unzipExtensions) {
		super(fields);
		UNZIP_EXTENSIONS = unzipExtensions;
	}
	
	@Override
	void parse(File f) throws IOException, SAXException {
		if (f.isFile() && UNZIP_EXTENSIONS.contains(FileUtil.getExtension(f.getName()))) {
			ZipDirectory zu = ZipDirectory.unzip(f);
				/*
				 * unfortunately, super.parse(zu) cannot be called here for two reasons:
				 * 1) ZIP tagname is needed
				 * 2) FileAttributes(f, _) is needed
				 */
				startElement(NS, TAG_ZIP, TAG_ZIP, new FileAttributes(f, FIELDS));
					for (File c : zu.listFiles())
						parse(c);
				endElement(NS, TAG_ZIP, TAG_ZIP);
			zu.zip();
		} else
			super.parse(f);
	}
	
	/** @return a source which parses with this class */
	public static SAXSource getZipSource(File f) {
		return new SAXSource(
				new ZipXMLReader(),
				getFileInputSource(f));
	}
}