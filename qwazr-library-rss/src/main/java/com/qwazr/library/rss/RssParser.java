/*
 * Copyright 2015-2020 Emmanuel Keller / QWAZR
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
package com.qwazr.library.rss;

import com.qwazr.extractor.ParserFactory;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserInterface;
import com.qwazr.extractor.ParserResult;
import com.qwazr.extractor.ParserUtils;
import static com.qwazr.extractor.ParserUtils.languageDetection;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndLink;
import com.rometools.rome.feed.synd.SyndPerson;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

public class RssParser implements ParserFactory, ParserInterface {

    private static final String NAME = "rss";

    private static final MediaType DEFAULT_MIMETYPE = MediaType.valueOf("application/rss+xml");

    private static final Collection<MediaType> DEFAULT_MIMETYPES = List.of(DEFAULT_MIMETYPE);

    private static final Collection<String> DEFAULT_EXTENSIONS = List.of("rss");

    final private static ParserField CHANNEL_TITLE = ParserField.newString("channel_title", "The title of the channel");

    final private static ParserField CHANNEL_LINK = ParserField.newString("channel_link", "The link of the channel");

    final private static ParserField CHANNEL_DESCRIPTION =
            ParserField.newString("channel_description", "The description of the channel");

    final private static ParserField CHANNEL_CATEGORY =
            ParserField.newString("channel_category", "The category of the channel");

    final private static ParserField CHANNEL_AUTHOR_NAME =
            ParserField.newString("channel_author_name", "The name of the author");

    final private static ParserField CHANNEL_AUTHOR_EMAIL =
            ParserField.newString("channel_author_email", "The email address of the author");

    final private static ParserField CHANNEL_CONTRIBUTOR_NAME =
            ParserField.newString("channel_contributor_name", "The name of the contributor");

    final private static ParserField CHANNEL_CONTRIBUTOR_EMAIL =
            ParserField.newString("channel_contributor_email", "The email address of the contributor");

    final private static ParserField CHANNEL_PUBLISHED_DATE =
            ParserField.newString("channel_published_date", "The published date of the channel");

    final private static ParserField ATOM_TITLE = ParserField.newString("atom_title", "The title of the atom");

    final private static ParserField ATOM_LINK = ParserField.newString("atom_link", "The link of the atom");

    final private static ParserField ATOM_DESCRIPTION =
            ParserField.newString("atom_description", "The description of the atom");

    final private static ParserField ATOM_CATEGORY = ParserField.newString("atom_category", "The category of the atom");

    final private static ParserField ATOM_AUTHOR_NAME =
            ParserField.newString("atom_author_name", "The name of the author");

    final private static ParserField ATOM_AUTHOR_EMAIL =
            ParserField.newString("atom_author_email", "The email address of the author");

    final private static ParserField ATOM_CONTRIBUTOR_NAME =
            ParserField.newString("atom_contributor_name", "The name of the contributor");

    final private static ParserField ATOM_CONTRIBUTOR_EMAIL =
            ParserField.newString("atom_contributor_email", "The email address of the contributor");

    final private static ParserField ATOM_PUBLISHED_DATE =
            ParserField.newString("atom_published_date", "The published date");

    final private static ParserField ATOM_UPDATED_DATE = ParserField.newString("atom_updated_date", "The updated date");

    final private static Collection<ParserField> FIELDS = Arrays.asList(CHANNEL_TITLE,
            CHANNEL_LINK,
            CHANNEL_DESCRIPTION,
            CHANNEL_CATEGORY,
            CHANNEL_AUTHOR_NAME,
            CHANNEL_AUTHOR_EMAIL,
            CHANNEL_CONTRIBUTOR_NAME,
            CHANNEL_CONTRIBUTOR_EMAIL,
            CHANNEL_PUBLISHED_DATE,
            ATOM_TITLE,
            ATOM_LINK,
            ATOM_DESCRIPTION,
            ATOM_AUTHOR_NAME,
            ATOM_AUTHOR_EMAIL,
            ATOM_CONTRIBUTOR_NAME,
            ATOM_CONTRIBUTOR_EMAIL,
            ATOM_PUBLISHED_DATE,
            ATOM_UPDATED_DATE,
            LANG_DETECTION);

    @Override
    public Collection<ParserField> getFields() {
        return FIELDS;
    }

    @Override
    public Collection<String> getSupportedFileExtensions() {
        return DEFAULT_EXTENSIONS;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ParserInterface createParser() {
        return this;
    }

    @Override
    public Collection<MediaType> getSupportedMimeTypes() {
        return DEFAULT_MIMETYPES;
    }

    private void addPersons(ParserField nameField, ParserField emailField, List<SyndPerson> persons,
                            ParserResult.FieldsBuilder parserDocument) {
        if (persons == null)
            return;
        for (SyndPerson person : persons) {
            parserDocument.add(nameField, person.getName());
            parserDocument.add(emailField, person.getEmail());
        }
    }

    private void addLinks(final ParserField linkField, final List<SyndLink> links,
                          final ParserResult.FieldsBuilder parserDocument) {
        if (links == null)
            return;
        for (SyndLink link : links)
            parserDocument.add(linkField, link.getHref());
    }

    private void addCategories(final ParserField categoryField, final List<SyndCategory> categories,
                               final ParserResult.FieldsBuilder parserDocument) {
        if (categories == null)
            return;
        for (SyndCategory category : categories)
            parserDocument.add(categoryField, category.getName());
    }

    private void addContent(final ParserField atomDescription,
                            final SyndContent content,
                            final ParserResult.FieldsBuilder result) {
        if (content == null)
            return;
        final String value = content.getValue();
        if (value == null)
            return;
        result.add(atomDescription, value);
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final InputStream inputStream,
                                final MediaType mediaType) throws IOException {

        final ParserResult.Builder builder = ParserResult.of(NAME);
        final SyndFeedInput input = new SyndFeedInput();
        try (final XmlReader reader = new XmlReader(inputStream)) {
            SyndFeed feed = input.build(reader);
            if (feed == null)
                return builder.build();

            final ParserResult.FieldsBuilder metas = builder.metas();
            metas.set(MIME_TYPE, mediaType.toString());
            metas.add(CHANNEL_TITLE, feed.getTitle());
            metas.add(CHANNEL_DESCRIPTION, feed.getDescription());

            addPersons(CHANNEL_AUTHOR_NAME, CHANNEL_AUTHOR_EMAIL, feed.getAuthors(), metas);
            addPersons(CHANNEL_CONTRIBUTOR_NAME, CHANNEL_CONTRIBUTOR_EMAIL, feed.getContributors(), metas);
            addLinks(CHANNEL_LINK, feed.getLinks(), metas);
            addCategories(CHANNEL_CATEGORY, feed.getCategories(), metas);

            metas.add(CHANNEL_PUBLISHED_DATE, feed.getPublishedDate());

            List<SyndEntry> entries = feed.getEntries();
            if (entries == null)
                return builder.build();

            for (SyndEntry entry : entries) {

                final ParserResult.FieldsBuilder result = builder.newDocument();

                result.add(ATOM_TITLE, entry.getTitle());
                addContent(ATOM_DESCRIPTION, entry.getDescription(), result);
                addPersons(ATOM_AUTHOR_NAME, ATOM_AUTHOR_EMAIL, entry.getAuthors(), result);
                addPersons(ATOM_CONTRIBUTOR_NAME, ATOM_CONTRIBUTOR_EMAIL, entry.getContributors(), result);
                addLinks(ATOM_LINK, entry.getLinks(), result);
                result.add(ATOM_LINK, entry.getLink());
                addCategories(ATOM_CATEGORY, entry.getCategories(), result);
                result.add(ATOM_PUBLISHED_DATE, entry.getPublishedDate());
                result.add(ATOM_UPDATED_DATE, entry.getUpdatedDate());
                // Apply the language detection
                result.add(LANG_DETECTION, languageDetection(result, ATOM_DESCRIPTION, 10000));
            }
        } catch (FeedException e) {
            throw new InternalServerErrorException(e);
        }
        return builder.build();
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final Path filePath) throws IOException {
        return ParserUtils.toBufferedStream(filePath, in -> extract(parameters, in, DEFAULT_MIMETYPE));
    }

}
