package org.fwb.file.xml.todo;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.fwb.file.ByteSizeFormat;
import org.fwb.file.FileUtil;
import org.fwb.xml.sax.snax.SmartContentHandler;

/**
 * this generates a "snapshot" of a filesystem directory, just like Flux, with three differences.
 * directories include information about size and contained count, recursively.
 * those are the *only* data supported (along with name/path).
 * filtering by minimum size is available.
 * 
 * @deprecated TODO for instance in the .xml or a .dir package?
 */
class FindLargeFiles extends XMLFilterImpl {
	static final Logger LOG = LoggerFactory.getLogger(FindLargeFiles.class);
	
	public static final void main(String[] args) throws Throwable {
		try {
			File[] f = getFiles(args);
			if (f == null) {
				System.err.println("cancelled!");
				Thread.sleep(5 * 1000);
				return;
			}
			
			System.out.println(f[0]);
			System.out.println(f[1]);
			
			FindLargeFiles flf = new FindLargeFiles();
			flf.setContentHandler(new SmartContentHandler(f[1]));
			flf.parse(f[0]);
			
			JOptionPane.showMessageDialog(null, "success!", "info", JOptionPane.INFORMATION_MESSAGE);
		} catch (Throwable t) {
			t.printStackTrace();
			JOptionPane.showMessageDialog(null, t.toString(), "error!", JOptionPane.ERROR_MESSAGE);
			throw t;
		}
	}
	
	/**
	 * utility method, given (optional, arbitrarily-ordered) main arguments,
	 * return null (for any issues) or exactly 2 Files, in-order: root directory, report file.
	 */
	public static final File[] getFiles(String[] args) {
		File[] retVal = new File[2];
		for (String s : args == null ? new String[0] : args) {
			if (s != null) {
				File f = new File(s);
				if (f.isDirectory()) {
					if (retVal[0] == null)
						retVal[0] = f;
				} else {
					if (retVal[1] == null)
						retVal[1] = f;
				}
			}
		}
		
		if (retVal[0] == null)
			retVal[0] = getFile();
		if (retVal[0] != null && retVal[1] == null)
			retVal[1] = saveFile(retVal[0]);
		
		if (retVal[0] == null || retVal[1] == null)
			retVal = null;
		
		return retVal;
	}
	/** @deprecated see todo/io package */
	static File getFile() {
		int mode = JFileChooser.DIRECTORIES_ONLY;
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(mode);
		int returnVal = chooser.showOpenDialog(null);
		return returnVal == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
	}
	static File saveFile(File inputFile) {
		Preconditions.checkNotNull(inputFile);
		Preconditions.checkArgument(inputFile.isDirectory());
		File outputFile = new File(
				inputFile,
				String.format("FLF.%s.xml", FileUtil.seconds().format(new Date())));
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(outputFile);
		int returnVal = chooser.showSaveDialog(null);
		return returnVal == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
	}
	
//	public static final void main(String[] args) throws Throwable {
//		File f = args == null || args.length < 1 ? Dialogs.getFile() : new File(args[0]);
//		
//		try {
//			// get Report File and Report Root
//			final File
//				root, rpt;
//			
//			if (f == null) {
//				System.out.println("cancelled");
//				return;
//			}
//			if (f.isDirectory()) {
//				root = f;
//				rpt = new File(root, "FLF." + new FileSize().format(Long.getLong("org.fwb.prod.util.fs.DEFAULT_MIN", DEFAULT_MIN)) + "." + TimeStamp.secs().format(new Date()) + ".xml");
//			} else {
//				root = f.getParentFile();
//				rpt = f;
//			}
//			System.out.println("report: " + rpt);
//			
//			FindLargeFiles flf = new FindLargeFiles();
//			flf.setContentHandler(new SmartContentHandler(rpt));
//			flf.parse(root);
//			
//			Dialogs.say("success!");
//		} catch (Throwable t) {
//			t.printStackTrace();
//			Dialogs.say("error!");
//			throw t;
//		}
//	}
	
	/* CONSTANTS */
	/** default lower limit: 0 (show all Files) */
	private static final long DEFAULT_MIN = 0;
//		DEFAULT = 128 * FileSize.MB;	// 128M
	/** default StandAlone */
	private static final boolean DEFAULT_SA = true;
	
	/* INSTANCE */
	final long MIN;
	final boolean SA;
	final String FMT;
	
	public FindLargeFiles() {
		this(
			Long.getLong(
					String.format("%s.%s", FindLargeFiles.class.getName(), "DEFAULT_MIN"),
					DEFAULT_MIN),
			Boolean.parseBoolean(
					System.getProperty(
							String.format("%s.%s", FindLargeFiles.class.getName(), "DEFAULT_SA"),
							Boolean.toString(DEFAULT_SA))),
			null
		);
	}
	
	public FindLargeFiles(long min, boolean standAlone, String sizeFormat) {
		MIN = min;
		SA = standAlone;
		FMT = sizeFormat;
	}
	
	@Override
	public final void parse(InputSource is) throws IOException, SAXException {
		File f;
		try {
			URI u = new URI(is.getSystemId());	// URISyntaxException
			f = new File(u);					// IllegalArgumentException
		} catch (URISyntaxException e) {
//			IOException ioe = new IOException();
//			ioe.initCause(e);
//			throw ioe;
			throw new IOException(e);	// >= Java 1.6
		} catch (IllegalArgumentException e) {
//			IOException ioe = new IOException();
//			ioe.initCause(e);
//			throw ioe;
			throw new IOException(e);	// >= Java 1.6
		}
		
		parse(f);
	}
	
	/**
	 * calls {@link #parse(File, ContentHandler, long, boolean, String)} with arguments
	 * ({@code f}, {@link #getContentHandler()}, {@link #MIN}, {@link #SA}, {@link #FMT}.
	 */
	public final void parse(File f) throws SAXException {
		parse(f, getContentHandler());
	}
	
	public static final void parse(File f, ContentHandler ch) throws SAXException {
		parse(f, ch,
			DEFAULT_MIN, DEFAULT_SA, new ByteSizeFormat(), FileUtil.timestamp());
	}
	/**
	 * @param standAlone	true if this method should "initialize" and "complete" the parse with <code>startDocument</code> and <code>endDocument</code> events; false otherwise
	 */
	public static final void parse(File f, ContentHandler ch, long min, boolean standAlone, NumberFormat sizeFormat, DateFormat timeFormat) throws SAXException {
		SmartContentHandler sch = SmartContentHandler.of(ch);
		
		try {
			f = f.getCanonicalFile();
		} catch (IOException e) {
			f = f.getAbsoluteFile();
		}
		
		// build
		Date date = new Date();
		long st = date.getTime();
		LOG.info("began parse");
		Spec s = new Spec(f);
		LOG.info("in " + (System.currentTimeMillis() - st) + "ms built Spec(" + f + ")");
		
		if (standAlone)
			sch.startDocument();
		
		sch.startElement("flf");
		sch.addAttribute("f", f.getPath());
		sch.addAttribute("min", sizeFormat.format(min));
		sch.addAttribute("start", timeFormat.format(date));
		
		// parse
		st = System.currentTimeMillis();
		serialize(s, sch, min, sizeFormat, new DecimalFormat("#,##0"));
		LOG.info("in " + (System.currentTimeMillis() - st) + "ms converted to XML " + s);
		
		sch.endElement();
		
		// serialize
		if (standAlone) {
			st = System.currentTimeMillis();
			sch.endDocument();
			LOG.info("in " + (System.currentTimeMillis() - st) + "ms serialized " + s);
		}
	}
	
	// recursive
	private static final void serialize(Spec f, SmartContentHandler sch, long min, NumberFormat size, DecimalFormat countFormat) throws SAXException {
		if (f.LENGTH >= min || f.ERR) {
				sch.startElement(f.KIDS == null ? (f.ERR ? "err" : "file") : "dir");
				sch.addAttribute("name", f.NAME);
				sch.addAttribute("size", size.format(f.LENGTH));
				if (f.KIDS != null) {
					sch.addAttribute("count", countFormat.format(f.COUNT));
					for (Spec c : f.KIDS)
							serialize(c, sch, min, size, countFormat);
				}
			sch.endElement();
		}
	}
	
	private static final class Spec implements Comparable<Spec> {
		public final Spec PARENT;
		public final boolean ERR;
		public final String NAME;
		public final SortedSet<Spec> KIDS;
		public final long LENGTH;
		/** count of descendant files */
		public final int COUNT;
		
		public Spec(File f) {
			this(f, null);
		}
		
		private Spec(File f, Spec parent) {
			PARENT = parent;
			NAME = f.getName();
//			NAME = parent == null ? f.getPath() : f.getName();
			if (f.isDirectory()) {
				File[] kids = f.listFiles();
				long length = 0;
				int count = 0;
				ERR = kids == null;
				if (ERR)
					KIDS = null;
				else {
					KIDS = new TreeSet<Spec>(Collections.reverseOrder());
					
					for (File c : kids) {
						Spec s = new Spec(c, this);
						KIDS.add(s);
						length += s.LENGTH;
						count += 1 + s.COUNT;
					}
				}
				LENGTH = length;
				COUNT = count;
			} else {
				ERR = false;
				KIDS = null;
				LENGTH = f.length();
				COUNT = 0;
			}
		}
		
		public final String toString() {
			return "Spec" + (ERR ? "!" : "") + "(" + NAME + ", " + new ByteSizeFormat().format(LENGTH) + ", " + (KIDS == null ? "null" : "KIDS") + ")";
		}
		
		public int compareTo(Spec s) {
			return compareTo(this, s);
		}
		
		public static final int compareTo(Spec s1, Spec s2) {
			if (s1 == null && s2 == null)
				return 0;
			
			int retVal = integer((s1 == null ? 0 : s1.LENGTH) - (s2 == null ? 0 : s2.LENGTH));
			
			if (retVal == 0)
				retVal = (s1 == null ? "" : s1.NAME).compareTo(s2 == null ? "" : s2.NAME);
			
			// since a file can have a blank name, neither can be null at this point (or the other would have given nonzero diff)
			if (retVal == 0)
				retVal = compareTo(s1.PARENT, s2.PARENT);
			
			return retVal;
		}
		
		/** cast long to int, avoiding overflow */
		public static final int integer(long l) {
			return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, l));
		}
	}
}