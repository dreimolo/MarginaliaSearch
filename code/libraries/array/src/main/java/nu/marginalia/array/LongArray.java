package nu.marginalia.array;

import com.upserve.uppend.blobs.NativeIO;
import nu.marginalia.array.algo.LongArrayBase;
import nu.marginalia.array.algo.LongArraySearch;
import nu.marginalia.array.algo.LongArraySort;
import nu.marginalia.array.algo.LongArrayTransformations;
import nu.marginalia.array.delegate.ShiftedLongArray;
import nu.marginalia.array.page.SegmentLongArray;

import java.io.IOException;
import java.lang.foreign.Arena;


public interface LongArray extends LongArrayBase, LongArrayTransformations, LongArraySearch, LongArraySort, AutoCloseable {
    int WORD_SIZE = 8;

    @Deprecated
    static LongArray allocate(long size) {
        return SegmentLongArray.onHeap(Arena.ofShared(), size);
    }

    default LongArray shifted(long offset) {
        return new ShiftedLongArray(offset, this);
    }
    default LongArray range(long start, long end) {
        return new ShiftedLongArray(start, end, this);
    }

    /** Translate the range into the equivalent range in the underlying array if they are in the same page */
    ArrayRangeReference<LongArray> directRangeIfPossible(long start, long end);

    void force();
    void close();

    void advice(NativeIO.Advice advice) throws IOException;
    void advice(NativeIO.Advice advice, long start, long end) throws IOException;
}
