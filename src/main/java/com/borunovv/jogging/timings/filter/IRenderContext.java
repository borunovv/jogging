package com.borunovv.jogging.timings.filter;

public interface IRenderContext {
    void write(String str);
    void comparison(String column, String cmpOperation, String value);
}
