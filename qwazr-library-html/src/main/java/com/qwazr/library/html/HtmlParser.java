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
package com.qwazr.library.html;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qwazr.extractor.ParserAbstract;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserFieldsBuilder;
import com.qwazr.extractor.ParserResultBuilder;
import com.qwazr.utils.DomUtils;
import com.qwazr.utils.HtmlUtils;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.XPathParser;
import org.apache.xerces.parsers.DOMParser;
import org.cyberneko.html.HTMLConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import se.fishtank.css.selectors.Selectors;
import se.fishtank.css.selectors.dom.W3CNode;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlParser extends ParserAbstract {

    /**
     * Create a new NekoHTML configuration
     *
     * @return
     */
    static public HTMLConfiguration getNewHtmlConfiguration() {
        final HTMLConfiguration config = new HTMLConfiguration();
        config.setFeature("http://xml.org/sax/features/namespaces", true);
        config.setFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content", false);
        config.setFeature("http://cyberneko.org/html/features/balance-tags", true);
        config.setFeature("http://cyberneko.org/html/features/report-errors", false);
        config.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
        config.setProperty("http://cyberneko.org/html/properties/names/attrs", "lower");
        return config;
    }

    static public DOMParser getNewDomParser() {
        return new DOMParser(getNewHtmlConfiguration());
    }

    private final static ThreadLocal<DOMParser> DOM_PARSER_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> getNewDomParser());

    static public DOMParser getThreadLocalDomParser() {
        return DOM_PARSER_THREAD_LOCAL.get();
    }

    final private static String[] DEFAULT_MIMETYPES = { "text/html" };

    final private static String[] DEFAULT_EXTENSIONS = { "htm", "html" };
    final private static ParserField HEADERS =
            ParserField.newString("headers", "Extract headers (h1, h2, h3, h4, h5, h6)");

    final private static ParserField H1 = ParserField.newString("h1", "H1 header contents");

    final private static ParserField H2 = ParserField.newString("h2", "H2 header contents");

    final private static ParserField H3 = ParserField.newString("h3", "H3 header contents");

    final private static ParserField H4 = ParserField.newString("h4", "H4 header contents");

    final private static ParserField H5 = ParserField.newString("h5", "H5 header contents");

    final private static ParserField H6 = ParserField.newString("h6", "H6 header contents");

    final private static ParserField ANCHORS = ParserField.newString("anchors", "Anchors");

    final private static ParserField IMAGES = ParserField.newMap("images", "Image tags");

    final private static ParserField METAS = ParserField.newMap("metas", "Meta tags");

    final private static ParserField SELECTORS = ParserField.newMap("selectors", "Selector results");

    final private static ParserField[] FIELDS =
            { TITLE, CONTENT, H1, H2, H3, H4, H5, H6, ANCHORS, IMAGES, METAS, LANG_DETECTION, SELECTORS };

    final private static ParserField XPATH_PARAM = ParserField.newString("xpath", "Any XPATH selector");

    final private static ParserField XPATH_NAME_PARAM =
            ParserField.newString("xpath_name", "The name of the XPATH selector");

    final private static ParserField CSS_PARAM = ParserField.newString("css", "Any CSS selector");

    final private static ParserField CSS_NAME_PARAM = ParserField.newString("css_name", "The name of the CSS selector");

    final private static ParserField REGEXP_PARAM = ParserField.newString("regexp", "Any regular expression");

    final private static ParserField REGEXP_NAME_PARAM =
            ParserField.newString("regexp_name", "The name of the regular expression");

    final private static ParserField[] PARAMETERS = { TITLE,
            CONTENT,
            HEADERS,
            ANCHORS,
            IMAGES,
            METAS,
            XPATH_PARAM,
            XPATH_NAME_PARAM,
            CSS_PARAM,
            CSS_NAME_PARAM,
            REGEXP_PARAM,
            REGEXP_NAME_PARAM };

    @Override
    public ParserField[] getParameters() {
        return PARAMETERS;
    }

    @Override
    public ParserField[] getFields() {
        return FIELDS;
    }

    private void extractTitle(final XPathParser xpath, final Document documentElement,
            final ParserFieldsBuilder document) throws XPathExpressionException {
        final String title = xpath.evaluateString(documentElement, "/html/head/title//text()");
        if (title != null)
            document.set(TITLE, title);
    }

    private void extractHeaders(final Document documentElement, final ParserFieldsBuilder document) {
        addToField(document, H1, documentElement.getElementsByTagName("h1"));
        addToField(document, H2, documentElement.getElementsByTagName("h2"));
        addToField(document, H3, documentElement.getElementsByTagName("h3"));
        addToField(document, H4, documentElement.getElementsByTagName("h4"));
        addToField(document, H5, documentElement.getElementsByTagName("h5"));
        addToField(document, H6, documentElement.getElementsByTagName("h6"));
    }

    private void extractAnchors(final XPathParser xpath, final Document documentElement,
            final ParserFieldsBuilder document) throws XPathExpressionException {
        DomUtils.forEach(xpath.evaluateNodes(documentElement, "//a/@href"),
                node -> document.add(ANCHORS, DomUtils.getAttributeString(node, "href")));
    }

    private void extractImgTags(final Document documentElement, final ParserFieldsBuilder document) {
        DomUtils.forEach(documentElement.getElementsByTagName("img"), node -> {
            final Map<String, String> map = new LinkedHashMap<>();
            addToMap(map, "src", DomUtils.getAttributeString(node, "src"));
            addToMap(map, "alt", DomUtils.getAttributeString(node, "alt"));
            if (!map.isEmpty())
                document.add(IMAGES, map);
        });
    }

    private void extractTextContent(final Document documentElement, final ParserFieldsBuilder document) {
        HtmlUtils.domTextExtractor(documentElement, line -> document.add(CONTENT, line));
        // Lang detection
        document.add(LANG_DETECTION, languageDetection(document, CONTENT, 10000));
    }

    private void extractMeta(final Document documentElement, final ParserFieldsBuilder document) {
        NodeList nodeList = documentElement.getElementsByTagName("head");
        if (nodeList == null || nodeList.getLength() == 0)
            return;
        final Node head = nodeList.item(0);
        if (head.getNodeType() != Node.ELEMENT_NODE)
            return;
        final Map<String, String> map = new LinkedHashMap<>();
        DomUtils.forEach((((Element) head).getElementsByTagName("meta")), meta -> {
            final String name = DomUtils.getAttributeString(meta, "name");
            final String content = DomUtils.getAttributeString(meta, "content");
            if (!StringUtils.isEmpty(name) && !StringUtils.isEmpty(content))
                map.put(name, content);
        });
        if (!map.isEmpty())
            document.add(METAS, map);
    }

    private static class ListConsumer extends ArrayList<Object> implements XPathParser.Consumer {

        @Override
        @JsonIgnore
        public void accept(Node object) {
            accept(object.getTextContent());
        }

        @Override
        @JsonIgnore
        public void accept(Boolean object) {
            add(object);
        }

        @Override
        @JsonIgnore
        public void accept(String object) {
            if (object != null)
                add(object.trim());
        }

        @Override
        @JsonIgnore
        public void accept(Number object) {
            add(object);
        }

    }

    private Map<String, String> extractPrefixParameters(final MultivaluedMap<String, String> multivaluedMap,
            final ParserField selectorPrefix, final ParserField namePrefix) {
        final Map<String, String> parameters = new LinkedHashMap<>();
        int i = 0;
        String value;
        while ((value = getParameterValue(multivaluedMap, selectorPrefix, i)) != null) {
            final String name = getParameterValue(multivaluedMap, namePrefix, i);
            parameters.put(name == null ? Integer.toString(i) : name, value);
            i++;
        }
        return parameters;
    }

    private void extractXPath(final Map<String, String> parameters, final XPathParser xPath, final Node htmlDocument,
            final LinkedHashMap<String, Object> selectorsResult) throws XPathExpressionException {
        for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
            final ListConsumer results = new ListConsumer();
            xPath.evaluate(htmlDocument, parameter.getValue(), results);
            selectorsResult.put(parameter.getKey(), results);
        }
    }

    private void extractCss(final Map<String, String> parameters, final Node htmlDocument,
            final LinkedHashMap<String, Object> selectorsResult) {
        final Selectors<Node, W3CNode> selectors = new Selectors<>(new W3CNode(htmlDocument));
        for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
            final ListConsumer results = new ListConsumer();
            selectors.querySelectorAll(parameter.getValue()).forEach(results::accept);
            selectorsResult.put(parameter.getKey(), results);
        }
    }

    private void extractRegExp(final Map<String, String> parameters, final String htmlSource,
            final LinkedHashMap<String, Object> selectorsResult) {
        for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
            final ListConsumer results = new ListConsumer();
            final Matcher matcher = Pattern.compile(parameter.getValue(), Pattern.DOTALL).matcher(htmlSource);
            final int groupCount = matcher.groupCount();
            while (matcher.find())
                for (int j = 1; j <= groupCount; j++)
                    results.accept(matcher.group(j));
            selectorsResult.put(parameter.getKey(), results);
        }
    }

    private void addToMap(final Map<String, String> map, final String name, final String value) {
        if (!StringUtils.isEmpty(value))
            map.put(name, value);
    }

    private void addToField(final ParserFieldsBuilder document, final ParserField parserField,
            final NodeList elements) {
        DomUtils.forEach(elements, node -> document.add(parserField, node.getTextContent()));
    }

    @Override
    public void parseContent(final MultivaluedMap<String, String> parameters, final InputStream inputStream,
            final String extension, final String mimeType, final ParserResultBuilder resultBuilder) {

        try {
            resultBuilder.metas().set(MIME_TYPE, DEFAULT_MIMETYPES[0]);

            final Map<String, String> xPathParams = extractPrefixParameters(parameters, XPATH_PARAM, XPATH_NAME_PARAM);
            final Map<String, String> cssParams = extractPrefixParameters(parameters, CSS_PARAM, CSS_NAME_PARAM);
            final Map<String, String> regexpParams =
                    extractPrefixParameters(parameters, REGEXP_PARAM, REGEXP_NAME_PARAM);
            final boolean isSelector = !(xPathParams.isEmpty() && cssParams.isEmpty() && regexpParams.isEmpty());

            final DOMParser htmlParser = getThreadLocalDomParser();

            final String htmlSource;
            if (!regexpParams.isEmpty()) {
                htmlSource = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                htmlParser.parse(new InputSource(new StringReader(htmlSource)));
            } else {
                htmlSource = null;
                htmlParser.parse(new InputSource(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
            }

            final ParserFieldsBuilder parserDocument = resultBuilder.newDocument();

            final LinkedHashMap<String, Object> selectorsResult = new LinkedHashMap<>();

            final Document htmlDocument = htmlParser.getDocument();
            final XPathParser xPath = !xPathParams.isEmpty() || !isSelector ? new XPathParser() : null;

            if (!xPathParams.isEmpty())
                extractXPath(xPathParams, xPath, htmlDocument, selectorsResult);
            if (!cssParams.isEmpty())
                extractCss(cssParams, htmlDocument, selectorsResult);
            if (!regexpParams.isEmpty())
                extractRegExp(regexpParams, htmlSource, selectorsResult);

            final boolean selectorResultIsEmpty = selectorsResult.isEmpty();
            if (!selectorResultIsEmpty)
                parserDocument.set(SELECTORS, selectorsResult);

            if (selectorResultIsEmpty || (parameters != null && parameters.containsKey(TITLE.name)))
                extractTitle(xPath, htmlDocument, parserDocument);
            if (selectorResultIsEmpty || (parameters != null && parameters.containsKey(HEADERS.name)))
                extractHeaders(htmlDocument, parserDocument);
            if (selectorResultIsEmpty || (parameters != null && parameters.containsKey(ANCHORS.name)))
                extractAnchors(xPath, htmlDocument, parserDocument);
            if (selectorResultIsEmpty || (parameters != null && parameters.containsKey(IMAGES.name)))
                extractImgTags(htmlDocument, parserDocument);
            if (selectorResultIsEmpty || (parameters != null && parameters.containsKey(CONTENT.name)))
                extractTextContent(htmlDocument, parserDocument);
            if (selectorResultIsEmpty || (parameters != null && parameters.containsKey(METAS.name)))
                extractMeta(htmlDocument, parserDocument);
        } catch (IOException e) {
            throw convertIOException(e::getMessage, e);
        } catch (SAXException e) {
            throw convertException(e::getMessage, e);
        } catch (XPathExpressionException e) {
            throw new NotAcceptableException("Error in the XPATH expression: " + e.getMessage(), e);
        }
    }

    @Override
    public String[] getDefaultExtensions() {
        return DEFAULT_EXTENSIONS;
    }

    @Override
    public String[] getDefaultMimeTypes() {
        return DEFAULT_MIMETYPES;
    }

}
