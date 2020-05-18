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
package com.qwazr.library.freemarker;

import freemarker.cache.TemplateLoader;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class MultiTemplateLoader implements TemplateLoader {

    private final TemplateLoader[] loaders;

    MultiTemplateLoader(Builder builder) {
        Objects.requireNonNull(builder, "The builder is null");
        Objects.requireNonNull(builder.templateLoaders, "The templateLoaders is null");
        loaders = builder.templateLoaders.toArray(new TemplateLoader[0]);
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        for (TemplateLoader loader : loaders) {
            final Object object = loader.findTemplateSource(name);
            if (object != null)
                return new Item(loader, object);
        }
        return null;
    }

    @Override
    public long getLastModified(Object templateSource) {
        return ((Item) templateSource).getLastModified();
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        return ((Item) templateSource).getReader(encoding);
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        ((Item) templateSource).close();
    }

    public static Builder of(final TemplateLoader... loaders) {
        return new Builder().loader(loaders);
    }

    final private static class Item {

        private final TemplateLoader loader;
        private final Object object;

        private Item(TemplateLoader loader, Object object) {
            this.loader = loader;
            this.object = object;
        }

        long getLastModified() {
            return loader.getLastModified(object);
        }

        Reader getReader(String encoding) throws IOException {
            return loader.getReader(object, encoding);
        }

        void close() throws IOException {
            loader.closeTemplateSource(object);
        }
    }

    static class Builder {

        private Set<TemplateLoader> templateLoaders;

        public Builder loader(TemplateLoader... loaders) {
            if (loaders != null) {
                if (templateLoaders == null)
                    templateLoaders = new LinkedHashSet<>();
                templateLoaders.addAll(Arrays.asList(loaders));
            }
            return this;
        }

        public TemplateLoader build() {
            if (templateLoaders == null || templateLoaders.isEmpty())
                return null;
            if (templateLoaders.size() == 1)
                return templateLoaders.iterator().next();
            return new MultiTemplateLoader(this);
        }
    }
}
