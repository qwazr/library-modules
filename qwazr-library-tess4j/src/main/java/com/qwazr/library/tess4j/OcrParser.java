/*
 * Copyright 2016-2020 Emmanuel Keller / QWAZR
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
package com.qwazr.library.tess4j;

import com.qwazr.extractor.ParserFactory;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserInterface;
import com.qwazr.extractor.ParserResult;
import com.qwazr.extractor.ParserUtils;
import com.qwazr.utils.AutoCloseWrapper;
import com.qwazr.utils.LoggerUtils;
import com.qwazr.utils.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import net.sourceforge.tess4j.Tesseract1;
import net.sourceforge.tess4j.TesseractException;

public class OcrParser implements ParserFactory, ParserInterface {

    private static final Logger LOGGER = LoggerUtils.getLogger(OcrParser.class);

    private static final HashMap<MediaType, String> MIMEMAP;

    private static final String NAME = "ocr";

    static {
        MIMEMAP = new HashMap<>();
        MIMEMAP.put(MediaType.valueOf("image/tiff"), "tiff");
        MIMEMAP.put(MediaType.valueOf("image/jpeg"), "jpg");
        MIMEMAP.put(MediaType.valueOf("image/gif"), "gif");
        MIMEMAP.put(MediaType.valueOf("image/png"), "png");
        MIMEMAP.put(MediaType.valueOf("image/bmp"), "bmp");
        MIMEMAP.put(MediaType.valueOf("application/pdf"), "pdf");
    }

    final private static List<ParserField> FIELDS = List.of(CONTENT, LANG_DETECTION);

    final private static ParserField LANGUAGE = ParserField.newString("language",
            "The language code of the document if known: afr (Afrikaans) amh (Amharic) ara (Arabic) asm (Assamese) " +
                    "aze (Azerbaijani) aze_cyrl (Azerbaijani - Cyrilic) bel (Belarusian) ben (Bengali) " +
                    "bod (Tibetan) bos (Bosnian) bul (Bulgarian) cat (Catalan; Valencian) ceb (Cebuano) ces (Czech) " +
                    "chi_sim (Chinese - Simplified) chi_tra (Chinese - Traditional) chr (Cherokee) cym (Welsh) " +
                    "dan (Danish) dan_frak (Danish - Fraktur) deu (German) deu_frak (German - Fraktur) " +
                    "dzo (Dzongkha) ell (Greek, Modern (1453-)) eng (English) enm (English, Middle (1100-1500)) " +
                    "epo (Esperanto) equ (Math / equation detection module) est (Estonian) eus (Basque) " +
                    "fas (Persian) fin (Finnish) fra (French) frk (Frankish) frm (French, Middle (ca.1400-1600)) " +
                    "gle (Irish) glg (Galician) grc (Greek, Ancient (to 1453)) guj (Gujarati) " +
                    "hat (Haitian; Haitian Creole) heb (Hebrew) hin (Hindi) hrv (Croatian) hun (Hungarian) " +
                    "iku (Inuktitut) ind (Indonesian) isl (Icelandic) ita (Italian) ita_old (Italian - Old) " +
                    "jav (Javanese) jpn (Japanese) kan (Kannada) kat (Georgian) kat_old (Georgian - Old) " +
                    "kaz (Kazakh) khm (Central Khmer) kir (Kirghiz; Kyrgyz) kor (Korean) kur (Kurdish) lao (Lao) " +
                    "lat (Latin) lav (Latvian) lit (Lithuanian) mal (Malayalam) mar (Marathi) mkd (Macedonian) " +
                    "mlt (Maltese) msa (Malay) mya (Burmese) nep (Nepali) nld (Dutch; Flemish) nor (Norwegian) " +
                    "ori (Oriya) osd (Orientation and script detection module) pan (Panjabi; Punjabi) pol (Polish) " +
                    "por (Portuguese) pus (Pushto; Pashto) ron (Romanian; Moldavian; Moldovan) rus (Russian) " +
                    "san (Sanskrit) sin (Sinhala; Sinhalese) slk (Slovak) slk_frak (Slovak - Fraktur) " +
                    "slv (Slovenian) spa (Spanish; Castilian) spa_old (Spanish; Castilian - Old) sqi (Albanian) " +
                    "srp (Serbian) srp_latn (Serbian - Latin) swa (Swahili) swe (Swedish) syr (Syriac) tam (Tamil) " +
                    "tel (Telugu) tgk (Tajik) tgl (Tagalog) tha (Thai) tir (Tigrinya) tur (Turkish) " +
                    "uig (Uighur; Uyghur) ukr (Ukrainian) urd (Urdu) uzb (Uzbek) uzb_cyrl (Uzbek - Cyrilic) " +
                    "vie (Vietnamese) yid (Yiddish)");

    final private static List<ParserField> PARAMETERS = List.of(LANGUAGE);

    @Override
    public Collection<ParserField> getParameters() {
        return PARAMETERS;
    }

    @Override
    public Collection<ParserField> getFields() {
        return FIELDS;
    }

    @Override
    public Collection<String> getSupportedFileExtensions() {
        return null;
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
        return null;
    }

    private static final String TESSDATA_PREFIX;

    static {

        String s = System.getProperty("TESSDATA_PREFIX", System.getenv("TESSDATA_PREFIX"));
        if (StringUtils.isEmpty(s)) {
            // Unix/Linux Path
            Path p = Paths.get("/usr/share/tesseract");
            if (Files.exists(p) && Files.isDirectory(p))
                s = p.toString();
        }
        if (StringUtils.isEmpty(s)) {
            // MacOS path
            Path p = Paths.get("/usr/local/share");
            if (Files.exists(p) && Files.isDirectory(p))
                s = p.toString();
        }
        if (StringUtils.isEmpty(s)) {
            // Windows Path
            String pf = System.getenv("ProgramFiles");
            if (!StringUtils.isEmpty(pf)) {
                Path p = Paths.get(pf, "Tesseract-OCR");
                if (Files.exists(p) && Files.isDirectory(p))
                    s = p.toString();
            }
        }
        TESSDATA_PREFIX = s;
        LOGGER.info(() -> "TESSDATA_PREFIX sets to: " + TESSDATA_PREFIX);
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final Path filePath) {
        final ParserResult.Builder builder = ParserResult.of(NAME);
        final Tesseract1 tesseract = new Tesseract1();
        final String lang = ParserUtils.getParameterValue(parameters, LANGUAGE, 0);
        if (lang != null)
            tesseract.setLanguage(lang);
        if (TESSDATA_PREFIX != null)
            tesseract.setDatapath(TESSDATA_PREFIX);
        final String result;
        try {
            result = tesseract.doOCR(filePath.toFile());
        } catch (TesseractException e) {
            throw new InternalServerErrorException("A Tesseract error occurred: " + e.getMessage(), e);
        }
        if (!StringUtils.isEmpty(result)) {
            builder.newDocument().add(CONTENT, result);
        }
        return builder.build();
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final InputStream inputStream,
                                final MediaType mediaType) throws IOException {
        final String extension = MIMEMAP.get(mediaType);
        if (extension == null)
            throw new BadRequestException("The mime-type is not supported: " + mediaType);
        try (final AutoCloseWrapper<Path> a = AutoCloseWrapper.of(
                ParserUtils.createTempFile(inputStream, "." + extension), LOGGER, Files::deleteIfExists)) {
            return extract(parameters, a.get());
        }
    }

}
