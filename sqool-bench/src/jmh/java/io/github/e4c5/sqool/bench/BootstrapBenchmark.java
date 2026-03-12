package io.github.e4c5.sqool.bench;

import io.github.e4c5.sqool.core.SqoolCore;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BootstrapBenchmark {

  @Benchmark
  public String projectNameLookup() {
    return SqoolCore.PROJECT_NAME;
  }
}
