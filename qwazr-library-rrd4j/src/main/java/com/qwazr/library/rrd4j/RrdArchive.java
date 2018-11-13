/**
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
 **/
package com.qwazr.library.rrd4j;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.ArcDef;

import java.util.Objects;

public class RrdArchive {

    public final ConsolFun consolFun;
    public final Double xff;
    public final Integer steps;
    public final Integer rows;

    @JsonCreator
    private RrdArchive(@JsonProperty("consolFun") final ConsolFun consolFun, @JsonProperty("xff") final Double xff,
            @JsonProperty("steps") final Integer steps, @JsonProperty("rows") final Integer rows) {
        this.consolFun = consolFun;
        this.xff = xff;
        this.steps = steps;
        this.rows = rows;
    }

    @JsonIgnore
    ArcDef getDef() {
        Objects.requireNonNull(consolFun, "The consolFun property is required");
        Objects.requireNonNull(xff, "The xff property is required");
        Objects.requireNonNull(steps, "The steps property is required");
        Objects.requireNonNull(rows, "The rows property is required");
        return new ArcDef(consolFun, xff, steps, rows);
    }
}
