package com.borunovv.jogging.web;

import com.borunovv.core.web.HttpControllerFactory;
import org.springframework.stereotype.Service;

@Service
public class JoggingControllerFactory extends HttpControllerFactory {

    public JoggingControllerFactory() {
        super("com.borunovv.jogging.web.controllers");
    }
}
