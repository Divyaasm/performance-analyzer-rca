/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Merge_CurrentEvent extends Metric {
    public Merge_CurrentEvent(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.MERGE_CURRENT_EVENT.name(), evaluationIntervalSeconds);
    }
}
