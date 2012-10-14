/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

package org.elasticsearch.cluster.routing.allocation.decider;

import org.elasticsearch.cluster.routing.MutableShardRouting;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.common.settings.Settings;

import java.util.List;

import static org.elasticsearch.common.settings.ImmutableSettings.Builder.EMPTY_SETTINGS;

/**
 * An allocation strategy that does not allow for the same shard instance to be allocated on the same node.
 */
public class SameShardAllocationDecider extends SimpleAllocationDecider {

    public static final String SAME_HOST_SETTING = "cluster.routing.allocation.same_shard.host";

    private final boolean sameHost;

    public SameShardAllocationDecider() {
        this(EMPTY_SETTINGS);
    }
    
    public SameShardAllocationDecider(Settings settings) {
        this.sameHost = settings.getAsBoolean(SAME_HOST_SETTING, false);
    }

    @Override
    public Decision canAllocate(ShardRouting shardRouting, RoutingNode node, RoutingAllocation allocation) {
        List<MutableShardRouting> shards = node.shards();
        for (int i = 0; i < shards.size(); i++) {
            // we do not allow for two shards of the same shard id to exists on the same node
            if (shards.get(i).shardId().equals(shardRouting.shardId())) {
                return Decision.NO;
            }
        }
        if (sameHost) {
            if (node.node() != null) {
                for (RoutingNode checkNode : allocation.routingNodes()) {
                    if (checkNode.node() == null) {
                        continue;
                    }
                    // check if its on the same host as the one we want to allocate to
                    if (!checkNode.node().address().sameHost(node.node().address())) {
                        continue;
                    }
                    shards = checkNode.shards();
                    for (int i = 0; i < shards.size(); i++) {
                        if (shards.get(i).shardId().equals(shardRouting.shardId())) {
                            return Decision.NO;
                        }
                    }
                }
            }
        }
        return Decision.YES;
    }
}
