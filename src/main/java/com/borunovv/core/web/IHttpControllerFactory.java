package com.borunovv.core.web;

public interface IHttpControllerFactory {
    IHttpController findController(String path);
}
