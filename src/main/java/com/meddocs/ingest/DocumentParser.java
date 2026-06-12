package com.meddocs.ingest;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extracts plain text from an uploaded file. Apache Tika auto-detects the format (PDF, TXT,
 * MD, …) and pulls out the body text, so the rest of the pipeline only ever sees a String.
 */
@Component
public class DocumentParser {

	/**
	 * @param content     the raw file bytes
	 * @param contentType the declared MIME type (a hint for Tika's detection; may be null)
	 * @return the extracted plain text
	 * @throws DocumentParseException if the bytes can't be parsed as text
	 */
	public String parse(InputStream content, String contentType) {
		// -1 = no write limit, so large clinical documents aren't silently truncated.
		BodyContentHandler handler = new BodyContentHandler(-1);
		Metadata metadata = new Metadata();
		if (contentType != null) {
			metadata.set(Metadata.CONTENT_TYPE, contentType);
		}
		try {
			new AutoDetectParser().parse(content, handler, metadata, new ParseContext());
		} catch (Exception ex) {
			// Tika throws IOException/SAXException/TikaException — collapse to one domain error.
			throw new DocumentParseException("Failed to extract text from upload", ex);
		}
		return handler.toString().strip();
	}
}
