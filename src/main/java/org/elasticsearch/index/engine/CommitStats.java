/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.index.engine;

import org.apache.lucene.index.SegmentInfos;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;

import java.io.IOException;
import java.util.Map;

/** a class the returns dynamic information with respect to the last commit point of this shard */
public final class CommitStats implements Streamable, ToXContent {

    private Map<String, String> userData;
    private long generation;

    public CommitStats(SegmentInfos segmentInfos) {
        // clone the map to protect against concurrent changes
        userData = MapBuilder.<String, String>newMapBuilder().putAll(segmentInfos.getUserData()).immutableMap();
        // lucene calls the current generation, last generation.
        generation = segmentInfos.getLastGeneration();
    }

    private CommitStats() {

    }

    public static CommitStats readCommitStatsFrom(StreamInput in) throws IOException {
        CommitStats commitStats = new CommitStats();
        commitStats.readFrom(in);
        return commitStats;
    }

    public static CommitStats readOptionalCommitStatsFrom(StreamInput in) throws IOException {
        return in.readOptionalStreamable(new CommitStats());
    }


    public Map<String, String> getUserData() {
        return userData;
    }

    public long getGeneration() {
        return generation;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        MapBuilder<String, String> builder = MapBuilder.newMapBuilder();
        for (int i = in.readVInt(); i > 0; i--) {
            builder.put(in.readString(), in.readOptionalString());
        }
        userData = builder.immutableMap();
        generation = in.readLong();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeVInt(userData.size());
        for (Map.Entry<String, String> entry : userData.entrySet()) {
            out.writeString(entry.getKey());
            out.writeOptionalString(entry.getValue());
        }
        out.writeLong(generation);
    }

    static final class Fields {
        static final XContentBuilderString GENERATION = new XContentBuilderString("generation");
        static final XContentBuilderString USER_DATA = new XContentBuilderString("user_data");
        static final XContentBuilderString COMMIT = new XContentBuilderString("commit");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(Fields.COMMIT);
        builder.field(Fields.GENERATION, generation);
        builder.field(Fields.USER_DATA, userData);
        builder.endObject();
        return builder;
    }
}