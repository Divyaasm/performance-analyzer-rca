/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.samplers;


import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.commons.stats.collectors.SampleAggregator;
import org.opensearch.performanceanalyzer.commons.stats.emitters.ISampler;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;

public class MetricsDBFileSampler implements ISampler {
    private static final Logger LOG = LogManager.getLogger(MetricsDBFileSampler.class);
    private static final Path targetDirectoryPath;
    private static final DirectoryStream.Filter<Path> metricsdbFilter;
    private static final DirectoryStream.Filter<Path> metricsdbTarFilter;
    private final AppContext appContext;

    static {
        String metricsdbPrefix = MetricsDB.getFilePrefix();
        targetDirectoryPath = Paths.get(metricsdbPrefix).getParent();
        // Matches the metricsdb prefix concatenated with at least 1 digit
        // Example: /tmp/metricsdb_1607650180000
        String metricsdbPattern = "regex:" + metricsdbPrefix + "\\d+";
        // Matches the metricsdb prefix, less one character, concatenated with a suffix like
        // ".tar.2020-12-10-17-35.gz"
        // Example: /tmp/metricsdb.tar.2020-12-10-17-40.gz
        String metricsdbTarPattern =
                "regex:"
                        + metricsdbPrefix.substring(0, metricsdbPrefix.length() - 1)
                        + ".tar.\\d+-\\d+-\\d+-\\d+-\\d+.gz";
        PathMatcher metricsdbMatcher = FileSystems.getDefault().getPathMatcher(metricsdbPattern);
        PathMatcher metricsdbTarMatcher =
                FileSystems.getDefault().getPathMatcher(metricsdbTarPattern);
        metricsdbFilter = metricsdbMatcher::matches;
        metricsdbTarFilter = metricsdbTarMatcher::matches;
    }

    public MetricsDBFileSampler(final AppContext appContext) {
        Objects.requireNonNull(appContext);
        this.appContext = appContext;
    }

    @Override
    public void sample(SampleAggregator sampleCollector) {
        int numUncompressedMetricsdbFiles = 0;
        long sizeUncompressedMetricsdbFiles = 0;
        int numMetricsdbFiles = 0;
        long sizeMetricsdbFiles = 0;

        try (DirectoryStream<Path> metricsdbStream =
                        Files.newDirectoryStream(targetDirectoryPath, metricsdbFilter);
                DirectoryStream<Path> metricsdbTarStream =
                        Files.newDirectoryStream(targetDirectoryPath, metricsdbTarFilter)) {
            for (Path entry : metricsdbStream) {
                sizeUncompressedMetricsdbFiles += Files.size(entry);
                numUncompressedMetricsdbFiles += 1;
            }
            for (Path entry : metricsdbTarStream) {
                sizeMetricsdbFiles += Files.size(entry);
                numMetricsdbFiles += 1;
            }
        } catch (IOException e) {
            // Exceptions can arise here if the streams contain a path whose underlying file is
            // deleted before the path is
            // consumed.
            LOG.warn("Issue accessing metricsdb entries in {}", targetDirectoryPath);
            return;
        }

        numMetricsdbFiles += numUncompressedMetricsdbFiles;
        sizeMetricsdbFiles += sizeUncompressedMetricsdbFiles;

        sampleCollector.updateStat(ReaderMetrics.METRICSDB_NUM_FILES, numMetricsdbFiles);
        sampleCollector.updateStat(ReaderMetrics.METRICSDB_SIZE_FILES, sizeMetricsdbFiles);
        sampleCollector.updateStat(
                ReaderMetrics.METRICSDB_NUM_UNCOMPRESSED_FILES, numUncompressedMetricsdbFiles);
        sampleCollector.updateStat(
                ReaderMetrics.METRICSDB_SIZE_UNCOMPRESSED_FILES, sizeUncompressedMetricsdbFiles);
    }
}
