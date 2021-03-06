package us.shandian.giga.io;

import java.io.IOException;

import us.shandian.giga.streams.io.SharpStream;

public class ChunkFileInputStream extends SharpStream {

    private final long offset;
    private final long length;
    private SharpStream source;
    private long position;

    public ChunkFileInputStream(SharpStream target, long start) throws IOException {
        this(target, start, target.length());
    }

    public ChunkFileInputStream(SharpStream target, long start, long end) throws IOException {
        source = target;
        offset = start;
        length = end - start;
        position = 0;

        if (length < 1) {
            source.close();
            throw new IOException("The chunk is empty or invalid");
        }
        if (source.length() < end) {
            try {
                throw new IOException(String.format("invalid file length. expected = %s  found = %s", end, source.length()));
            } finally {
                source.close();
            }
        }

        source.seek(offset);
    }

    /**
     * Get absolute position on file
     *
     * @return the position
     */
    public long getFilePointer() {
        return offset + position;
    }

    @Override
    public int read() throws IOException {
        if ((position + 1) > length) {
            return 0;
        }

        int res = source.read();
        if (res >= 0) {
            position++;
        }

        return res;
    }

    @Override
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        if ((position + len) > length) {
            len = (int) (length - position);
        }
        if (len == 0) {
            return 0;
        }

        int res = source.read(b, off, len);
        position += res;

        return res;
    }

    @Override
    public long skip(long pos) throws IOException {
        pos = Math.min(pos + position, length);

        if (pos == 0) {
            return 0;
        }

        source.seek(offset + pos);

        long oldPos = position;
        position = pos;

        return pos - oldPos;
    }

    @Override
    public long available() {
        return (int) (length - position);
    }

    @SuppressWarnings("EmptyCatchBlock")
    @Override
    public void close() {
        source.close();
        source = null;
    }

    @Override
    public boolean isClosed() {
        return source == null;
    }

    @Override
    public void rewind() throws IOException {
        position = 0;
        source.seek(offset);
    }

    @Override
    public boolean canRewind() {
        return true;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public void write(byte value) {
    }

    @Override
    public void write(byte[] buffer) {
    }

    @Override
    public void write(byte[] buffer, int offset, int count) {
    }

}
