package com.almarel.orion.math;

import com.almarel.orion.framework.concurrent.AsyncExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Переиспользуемый сепарабельный гауссов блюр над плоским 3D float[] полем
 * (layout: x + z*width + y*width*length). Вынесен из SlopeSmoothingField
 * (#grass) как общий механизм — идентичная по структуре свёртка используется
 * и в DensityMatrix (#smooth), поэтому логика централизована здесь, чтобы
 * избежать дублирования (DRY) при будущем переиспользовании.
 */
public final class VolumetricBlur {

    private VolumetricBlur() {
    }

    private interface BlurTask {
        void compute(int start, int end);
    }

    public static float[] blur(float[] input, int width, int height, int length,
                               int radius, float[] kernel, AsyncExecutor asyncExecutor) {
        int volume = width * height * length;
        float[] temp = new float[volume];
        float[] output = new float[volume];

        int processors = Runtime.getRuntime().availableProcessors();
        int batchSize = Math.max(1000, (volume / processors) + 1);

        runParallel(volume, batchSize, asyncExecutor, (start, end) -> {
            for (int i = start; i < end; i++) {
                float sum = 0.0f;
                int x = i % width;
                for (int k = -radius; k <= radius; k++) {
                    int nx = x + k;
                    if (nx >= 0 && nx < width) {
                        float val = input[i + k];
                        if (val > 0) sum += val * kernel[k + radius];
                    }
                }
                temp[i] = sum;
            }
        });

        runParallel(volume, batchSize, asyncExecutor, (start, end) -> {
            for (int i = start; i < end; i++) {
                float sum = 0.0f;
                int z = (i / width) % length;
                for (int k = -radius; k <= radius; k++) {
                    int nz = z + k;
                    if (nz >= 0 && nz < length) {
                        float val = temp[i + k * width];
                        if (val > 0) sum += val * kernel[k + radius];
                    }
                }
                output[i] = sum;
            }
        });

        int strideY = width * length;

        runParallel(volume, batchSize, asyncExecutor, (start, end) -> {
            for (int i = start; i < end; i++) {
                float sum = 0.0f;
                int y = i / strideY;
                for (int k = -radius; k <= radius; k++) {
                    int ny = y + k;
                    if (ny >= 0 && ny < height) {
                        float val = output[i + k * strideY];
                        if (val > 0) sum += val * kernel[k + radius];
                    }
                }
                temp[i] = sum;
            }
        });

        return temp;
    }

    private static void runParallel(int volume, int batchSize, AsyncExecutor asyncExecutor, BlurTask taskLogic) {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < volume; i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, volume);
            tasks.add(() -> {
                taskLogic.compute(start, end);
                return null;
            });
        }
        try {
            List<Future<Void>> futures = asyncExecutor.getExecutor().invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }
}