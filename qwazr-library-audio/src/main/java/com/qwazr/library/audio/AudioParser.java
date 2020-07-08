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
package com.qwazr.library.audio;

import com.qwazr.extractor.ParserFactory;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserInterface;
import com.qwazr.extractor.ParserResult;
import com.qwazr.extractor.ParserUtils;
import com.qwazr.utils.AutoCloseWrapper;
import com.qwazr.utils.LoggerUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.StringUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.SupportedFileFormat;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagField;
import org.jaudiotagger.tag.TagTextField;

final public class AudioParser implements ParserFactory, ParserInterface {

    private final static Logger LOGGER = LoggerUtils.getLogger(AudioParser.class);

    private static final String NAME = "audio";

    private static final Map<MediaType, String> MIMEMAP;
    private static final Map<String, MediaType> EXTENSIONMAP;

    static {
        MIMEMAP = new HashMap<>();
        MIMEMAP.put(MediaType.valueOf("audio/ogg"), "ogg");
        MIMEMAP.put(MediaType.valueOf("audio/mpeg"), "mpg");
        MIMEMAP.put(MediaType.valueOf("audio/mpeg3"), "mp3");
        MIMEMAP.put(MediaType.valueOf("audio/flac"), "flac");
        MIMEMAP.put(MediaType.valueOf("audio/mp4"), "mp4");
        MIMEMAP.put(MediaType.valueOf("audio/vnd.rn-realaudio"), "ra");
        MIMEMAP.put(MediaType.valueOf("audio/x-pn-realaudio"), "ra");
        MIMEMAP.put(MediaType.valueOf("audio/x-realaudio"), "ra");
        MIMEMAP.put(MediaType.valueOf("audio/wav"), "wav");
        MIMEMAP.put(MediaType.valueOf("audio/x-wav"), "wav");
        MIMEMAP.put(MediaType.valueOf("audio/x-ms-wma"), "wma");

        EXTENSIONMAP = new HashMap<>();
        MIMEMAP.forEach((k, v) -> EXTENSIONMAP.putIfAbsent(v, k));
        EXTENSIONMAP.put("m4a", MediaType.valueOf("audio/mp4"));
    }

    private final static Map<FieldKey, ParserField> FIELDMAP;

    private final static ParserField FORMAT;

    private final static List<String> SUPPORTED_EXTENSIONS;

    static {
        // Build the list of extension for the FORMAT parameter
        StringBuilder sb = new StringBuilder("Supported format: ");
        boolean first = true;
        final List<String> extensionList = new ArrayList<>();
        for (final SupportedFileFormat sff : SupportedFileFormat.values()) {
            if (!first)
                sb.append(", ");
            else
                first = false;
            sb.append(sff.getFilesuffix());
            extensionList.add(sff.getFilesuffix());
        }

        SUPPORTED_EXTENSIONS = List.copyOf(extensionList);

        FORMAT = ParserField.newString("format", sb.toString());

        // Build the list of fields returned by the library
        FIELDMAP = new HashMap<>();
        for (FieldKey fieldKey : FieldKey.values())
            FIELDMAP.put(fieldKey, ParserField.newString(fieldKey.name().toLowerCase(), null));
    }

    private final static List<ParserField> PARAMETERS = List.of(FORMAT);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ParserInterface createParser() {
        return this;
    }

    @Override
    public Collection<ParserField> getParameters() {
        return PARAMETERS;
    }

    @Override
    public Collection<ParserField> getFields() {
        return FIELDMAP.values();
    }

    @Override
    public Collection<String> getSupportedFileExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    @Override
    public Collection<MediaType> getSupportedMimeTypes() {
        return MIMEMAP.keySet();
    }

    private ParserResult extract(final Path filePath,
                                 final MediaType mediaType) throws IOException {
        final ParserResult.Builder resultBuilder = ParserResult.of(NAME);
        try {
            final AudioFile f = AudioFileIO.read(filePath.toFile());
            final ParserResult.FieldsBuilder metas = resultBuilder.metas();
            if (mediaType != null)
                metas.set(MIME_TYPE, mediaType.toString());
            final Tag tag = f.getTag();
            if (tag == null)
                return resultBuilder.build();
            if (tag.getFieldCount() == 0)
                return resultBuilder.build();
            for (Map.Entry<FieldKey, ParserField> entry : FIELDMAP.entrySet()) {
                final List<TagField> tagFields = tag.getFields(entry.getKey());
                if (tagFields == null)
                    continue;
                for (TagField tagField : tagFields) {
                    if (!(tagField instanceof TagTextField))
                        continue;
                    metas.add(entry.getValue(), ((TagTextField) tagField).getContent());
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        }
        return resultBuilder.build();
    }


    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final Path filePath) throws IOException {
        return extract(filePath, EXTENSIONMAP.get(ParserUtils.getExtension(filePath)));
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final InputStream inputStream,
                                final MediaType mimeType) throws IOException {
        String format = ParserUtils.getParameterValue(parameters, FORMAT, 0);
        if (StringUtils.isEmpty(format))
            format = MIMEMAP.get(mimeType);
        if (StringUtils.isEmpty(format))
            throw new NotSupportedException("The format is not found");
        try (final AutoCloseWrapper<Path> a = AutoCloseWrapper.of(
                ParserUtils.createTempFile(inputStream, '.' + format), LOGGER,
                Files::deleteIfExists)) {
            return extract(a.get(), mimeType);
        }
    }

}
