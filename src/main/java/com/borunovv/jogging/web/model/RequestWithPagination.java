package com.borunovv.jogging.web.model;

public abstract class RequestWithPagination extends AbstractRequest {
    private static final long DEFAULT_OFFSET = 0L;
    private static final long DEFAULT_COUNT = 100L;

    private Long offset = 0L;
    private Long count = DEFAULT_COUNT;

    public long getOffset() {
        return offset != null ?
                offset :
                DEFAULT_OFFSET;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getCount() {
        return count != 0 ?
                count :
                DEFAULT_COUNT;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "RequestWithPagination{" +
                "offset=" + getOffset() +
                ", count=" + getCount() +
                "} " + super.toString();
    }
}
