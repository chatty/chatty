package com.pngencoder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class PngEncoderDeflaterExecutorService {
    public static int NUM_THREADS_IS_AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static class Holder {
        private static final ExecutorService INSTANCE = Executors.newFixedThreadPool(
                NUM_THREADS_IS_AVAILABLE_PROCESSORS,
                PngEncoderDeflaterExecutorServiceThreadFactory.getInstance());
    }
    static ExecutorService getInstance() {
        return Holder.INSTANCE;
    }

    private PngEncoderDeflaterExecutorService() {
    }
}
