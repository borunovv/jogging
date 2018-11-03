package com.borunovv.jogging;

import com.borunovv.core.log.Loggable;
import com.borunovv.core.server.nio.http.protocol.BasicAuth;
import com.borunovv.core.util.UrlReader;
import com.borunovv.jogging.config.JoggingApplicationProperties;
import com.borunovv.jogging.web.JoggingServer;
import org.apache.log4j.LogManager;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

public class Main {

    private static Loggable.MyLogger logger = Loggable.createLogger(Main.class);

    public static void main(String[] args) {
        String option = args.length > 0 ?
                args[0] :
                "";

        AbstractApplicationContext ctx = null;
        try {
            if (option.equalsIgnoreCase("stop")) {
                stopServer();
                return;
            }

            if (option.equalsIgnoreCase("start") || option.isEmpty()) {
                ctx = new ClassPathXmlApplicationContext("/application.xml");
                startServer(ctx);
            } else {
                throw new RuntimeException("Unrecognized option: '" + option + "'");
            }
        } catch (Exception e) {
            logger.error("Error starting server", e);
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            if (ctx != null) {
                ctx.close();
            }
            LogManager.shutdown();
        }
    }

    private static void startServer(AbstractApplicationContext ctx) {
        logger.info("Server starting..");
        JoggingServer server = ctx.getBean(JoggingServer.class);
        server.start();
    }

    private static void stopServer() {
        try {
            JoggingApplicationProperties properties = new JoggingApplicationProperties();
            String url = "localhost:" + properties.getHttpPort() + "/__stop";
            System.out.println("Stopping server using url: " + url);
            UrlReader.Request request = new UrlReader.Request(url);

            request.setExtraHeader(BasicAuth.AUTH_HEADER_NAME,
                    BasicAuth.makeAuthHeaderValue(
                            properties.getHttpLogin(), properties.getHttpPassword()));

            String response = UrlReader.send(request).getBodyAsString();
            System.out.println("Result: " + response);
        } catch (IOException ignore) {
            System.out.println("Server not running.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
