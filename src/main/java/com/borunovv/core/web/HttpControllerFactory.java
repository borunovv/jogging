package com.borunovv.core.web;

import com.borunovv.core.service.AbstractService;
import com.borunovv.core.util.ReflectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class HttpControllerFactory extends AbstractService
        implements ApplicationContextAware,  IHttpControllerFactory {

    private final String packageToScan;
    private ApplicationContext context;

    public HttpControllerFactory(String packageToScan) {
        this.packageToScan = packageToScan;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    private static final ConcurrentHashMap<String, IHttpController> controllers = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        Assert.notNull(context, "ApplicationContext not initialized!");

        // Ищем все контроллеры.
        Set<Class<?>> allControllersClasses = ReflectionUtils.scanAnnotatedClasses(
                packageToScan,
                HttpController.class,
                //Component.class,
                false);

        for (Class<?> clazz : allControllersClasses) {
            HttpController annotation = clazz.getAnnotation(HttpController.class);
            Assert.notNull(annotation, "Controller class must have annotation @HttpController: '" + clazz.getName() + "'");

            // Path в url для мапинга на него.
            String path = annotation.path().toLowerCase();

            if (StringUtils.isNotEmpty(path) && controllers.containsKey(path)) {
                throw new RuntimeException("Duplicate path for http controllers: '" + path + "'.\n"
                        + "See classes: "
                        + clazz.getName() + " and "
                        + controllers.get(path).getClass().getName());
            }

            // Убедимся, что он наследует IHttpController
            Assert.isTrue(IHttpController.class.isAssignableFrom(clazz),
                    "Class '" + clazz.getName() + "' must implement interface 'IHttpController'");

            controllers.put(path, (IHttpController) context.getBean(clazz));

            Assert.notNull(controllers.get(path), "Can't find bean for class '"
                    + clazz.getName() + "'. Ensure it is annotated by @Component or @Service.");

            logger.info("Registered http controller: " + clazz.getSimpleName() + " ('" + path + "')");
        }
    }

    @Override
    public IHttpController findController(String path) {
        path = path.toLowerCase();
        IHttpController controller = controllers.get(path);
        if (controller == null) {
            // Пробуем найти по началу
            String longestKey = null;
            for (String key : controllers.keySet()) {
                if (path.startsWith(key)) {
                    if (longestKey == null || longestKey.length() < key.length()) {
                        longestKey = key;
                    }
                }
            }
            if (longestKey != null) {
                controller = controllers.get(longestKey);
            }
        }
        return controller;
    }
}
