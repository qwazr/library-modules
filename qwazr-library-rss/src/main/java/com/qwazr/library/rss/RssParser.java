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

import com.qwazr.extractor.ParserAbstract;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserFieldsBuilder;
import com.qwazr.extractor.ParserResultBuilder;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndLink;
import com.rometools.rome.feed.synd.SyndPerson;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class RssParser extends ParserAbstract {

    private static final String[] DEFAULT_MIMETYPES = { "application/rss+xml" };

    private static final String[] DEFAULT_EXTENSIONS = { "rss" };

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

    final private static ParserField[] FIELDS = { CHANNEL_TITLE,
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

    private void addPersons(ParserField nameField, ParserField emailField, List<SyndPerson> persons,
            ParserFieldsBuilder parserDocument) {
        if (persons == null)
            return;
        for (SyndPerson person : persons) {
            parserDocument.add(nameField, person.getName());
            parserDocument.add(emailField, person.getEmail());
        }
    }

    private void addLinks(final ParserField linkField, final List<SyndLink> links,
            final ParserFieldsBuilder parserDocument) {
        if (links == null)
            return;
        for (SyndLink link : links)
            parserDocument.add(linkField, link.getHref());
    }

    private void addCategories(final ParserField categoryField, final List<SyndCategory> categories,
            final ParserFieldsBuilder parserDocument) {
        if (categories == null)
            return;
        for (SyndCategory category : categories)
            parserDocument.add(categoryField, category.getName());
    }

    private void addContent(final ParserField atomDescription, final SyndContent content,
            final ParserFieldsBuilder result) {
        if (content == null)
            return;
        final String value = content.getValue();
        if (value == null)
            return;
        result.add(atomDescription, value);
    }

    @Override
    public void parseContent(final MultivaluedMap<String, String> parameters, final InputStream inputStream,
            String extension, final String mimeType, final ParserResultBuilder resultBuilder) {

        final SyndFeedInput input = new SyndFeedInput();
        try (final XmlReader reader = new XmlReader(inputStream)) {
            SyndFeed feed = input.build(reader);
            if (feed == null)
                return;

            final ParserFieldsBuilder metas = resultBuilder.metas();
            metas.set(MIME_TYPE, DEFAULT_MIMETYPES[0]);
            metas.add(CHANNEL_TITLE, feed.getTitle());
            metas.add(CHANNEL_DESCRIPTION, feed.getDescription());

            addPersons(CHANNEL_AUTHOR_NAME, CHANNEL_AUTHOR_EMAIL, feed.getAuthors(), metas);
            addPersons(CHANNEL_CONTRIBUTOR_NAME, CHANNEL_CONTRIBUTOR_EMAIL, feed.getContributors(), metas);
            addLinks(CHANNEL_LINK, feed.getLinks(), metas);
            addCategories(CHANNEL_CATEGORY, feed.getCategories(), metas);

            metas.add(CHANNEL_PUBLISHED_DATE, feed.getPublishedDate());

            List<SyndEntry> entries = feed.getEntries();
            if (entries == null)
                return;

            for (SyndEntry entry : entries) {

                final ParserFieldsBuilder result = resultBuilder.newDocument();

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
        } catch (IOException e) {
            throw convertIOException(e::getMessage, e);
        } catch (FeedException e) {
            throw convertException(e::getMessage, e);
        }
    }

}
