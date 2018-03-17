/*
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qwazr.library.poi;

import com.qwazr.extractor.ParserAbstract;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserFieldsBuilder;
import com.qwazr.extractor.ParserResultBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.POIXMLProperties.CoreProperties;
import org.apache.poi.xslf.extractor.XSLFPowerPointExtractor;
import org.apache.poi.xslf.usermodel.DrawingParagraph;
import org.apache.poi.xslf.usermodel.DrawingTextBody;
import org.apache.poi.xslf.usermodel.DrawingTextPlaceholder;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFCommentAuthors;
import org.apache.poi.xslf.usermodel.XSLFComments;
import org.apache.poi.xslf.usermodel.XSLFCommonSlideData;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFSlideShow;
import org.openxmlformats.schemas.presentationml.x2006.main.CTComment;
import org.openxmlformats.schemas.presentationml.x2006.main.CTCommentAuthor;

import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PptxParser extends ParserAbstract {

	private static final String[] DEFAULT_MIMETYPES =
			{ "application/vnd.openxmlformats-officedocument.presentationml.presentation" };

	private static final String[] DEFAULT_EXTENSIONS = { "pptx" };

	final private static ParserField CREATOR = ParserField.newString("creator", "The name of the creator");

	final private static ParserField DESCRIPTION = ParserField.newString("description", null);

	final private static ParserField KEYWORDS = ParserField.newString("keywords", null);

	final private static ParserField SUBJECT = ParserField.newString("subject", "The subject of the document");

	final private static ParserField CREATION_DATE = ParserField.newDate("creation_date", null);

	final private static ParserField MODIFICATION_DATE = ParserField.newDate("modification_date", null);

	final private static ParserField SLIDES = ParserField.newString("slides", null);

	final private static ParserField MASTER = ParserField.newString("master", null);

	final private static ParserField NOTES = ParserField.newString("notes", null);

	final private static ParserField COMMENTS = ParserField.newString("comments", null);

	final private static ParserField[] FIELDS = { TITLE,
			CONTENT,
			CREATOR,
			DESCRIPTION,
			KEYWORDS,
			SUBJECT,
			CREATION_DATE,
			MODIFICATION_DATE,
			SLIDES,
			MASTER,
			NOTES,
			COMMENTS,
			LANG_DETECTION };

	@Override
	public ParserField[] getFields() {
		return FIELDS;
	}

	@Override
	public String[] getDefaultExtensions() {
		return DEFAULT_EXTENSIONS;
	}

	@Override
	public String[] getDefaultMimeTypes() {
		return DEFAULT_MIMETYPES;
	}

	@Override
	public void parseContent(final MultivaluedMap<String, String> parameters, final InputStream inputStream,
			String extension, final String mimeType, final ParserResultBuilder resultBuilder) throws Exception {
		if (StringUtils.isEmpty(extension))
			extension = ".pptx";
		final Path tempFile = ParserAbstract.createTempFile(inputStream, extension);
		try {
			parseContent(parameters, tempFile, extension, mimeType, resultBuilder);
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

	@Override
	public void parseContent(final MultivaluedMap<String, String> parameters, final Path filePath,
			final String extension, final String mimeType, final ParserResultBuilder resultBuilder) throws Exception {

		final XSLFSlideShow pptSlideShow = new XSLFSlideShow(filePath.toAbsolutePath().toString());
		final XMLSlideShow slideshow = new XMLSlideShow(pptSlideShow.getPackage());

		final ParserFieldsBuilder metas = resultBuilder.metas();
		metas.set(MIME_TYPE, findMimeType(extension, mimeType, this::findMimeTypeUsingDefault));

		// Extract metadata
		try (XSLFPowerPointExtractor poiExtractor = new XSLFPowerPointExtractor(slideshow)) {
			final CoreProperties info = poiExtractor.getCoreProperties();
			if (info != null) {
				metas.add(TITLE, info.getTitle());
				metas.add(CREATOR, info.getCreator());
				metas.add(SUBJECT, info.getSubject());
				metas.add(DESCRIPTION, info.getDescription());
				metas.add(KEYWORDS, info.getKeywords());
				metas.add(CREATION_DATE, info.getCreated());
				metas.add(MODIFICATION_DATE, info.getModified());
			}
		}
		extractSides(slideshow, resultBuilder);
	}

	/**
	 * Declined from XSLFPowerPointExtractor.java
	 */
	private String extractText(XSLFCommonSlideData data, boolean skipPlaceholders) {
		StringBuilder sb = new StringBuilder();
		for (DrawingTextBody textBody : data.getDrawingText()) {
			if (skipPlaceholders && textBody instanceof DrawingTextPlaceholder) {
				DrawingTextPlaceholder ph = (DrawingTextPlaceholder) textBody;
				if (!ph.isPlaceholderCustom()) {
					// Skip non-customised placeholder text
					continue;
				}
			}

			for (DrawingParagraph p : textBody.getParagraphs()) {
				sb.append(p.getText());
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	/**
	 * Declined from XSLFPowerPointExtractor.java
	 *
	 * @param slideshow
	 * @param resultBuilder
	 */
	private void extractSides(final XMLSlideShow slideshow, final ParserResultBuilder resultBuilder) {

		final List<XSLFSlide> slides = slideshow.getSlides();
		final XSLFCommentAuthors commentAuthors = slideshow.getCommentAuthors();

		for (XSLFSlide slide : slides) {

			// One document per slide
			final ParserFieldsBuilder result = resultBuilder.newDocument();

			final XSLFNotes notes = slide.getNotes();
			final XSLFComments comments = slide.getComments();
			final XSLFSlideLayout layout = slide.getSlideLayout();
			final XSLFSlideMaster master = layout.getSlideMaster();

			// TODO Do the slide's name
			// (Stored in docProps/app.xml)

			// Do the slide's text
			final String slideText = extractText(slide.getCommonSlideData(), false);
			result.add(SLIDES, slideText);
			result.add(CONTENT, slideText);

			// If requested, get text from the master and it's layout
			if (layout != null) {
				final String text = extractText(layout.getCommonSlideData(), true);
				result.add(MASTER, text);
				result.add(CONTENT, text);
			}
			if (master != null) {
				final String text = extractText(master.getCommonSlideData(), true);
				result.add(MASTER, text);
				result.add(CONTENT, text);
			}

			// If the slide has comments, do those too
			if (comments != null) {
				for (CTComment comment : comments.getCTCommentsList().getCmList()) {
					final StringBuilder sbComment = new StringBuilder();
					// Do the author if we can
					if (commentAuthors != null) {
						CTCommentAuthor author = commentAuthors.getAuthorById(comment.getAuthorId());
						if (author != null) {
							sbComment.append(author.getName());
							sbComment.append(": ");
						}
					}

					// Then the comment text, with a new line afterwards
					sbComment.append(comment.getText());
					sbComment.append("\n");
					if (sbComment.length() > 0) {
						final String text = sbComment.toString();
						result.add(COMMENTS, text);
						result.add(CONTENT, text);
					}
				}
			}

			// Do the notes if requested
			if (notes != null) {
				final String text = extractText(notes.getCommonSlideData(), false);
				result.add(NOTES, text);
				result.add(CONTENT, text);
			}

			result.add(LANG_DETECTION, languageDetection(result, CONTENT, 10000));

		}
	}
}
