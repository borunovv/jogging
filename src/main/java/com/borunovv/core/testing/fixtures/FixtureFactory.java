package com.borunovv.core.testing.fixtures;

import com.borunovv.core.util.IOUtils;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;

public class FixtureFactory implements IFixtureRepository {

    public FixtureFactory(DataSourceInfo dataSourceInfo) {
        this.dataSourceInfo = dataSourceInfo;
        Assert.notNull(dataSourceInfo, "dataSourceInfo must not be null.");
    }

    public FixtureFactory(DataSource dataSource) {
        this.dataSource = dataSource;
        Assert.notNull(dataSource, "dataSource must not be null.");
    }

    public AbstractFixture create(String fileName) {
        Assert.isTrue(dataSource != null || dataSourceInfo != null,
                "both dataSource and dataSourceInfo are null.");

        FileType fileType = getFileType(fileName);

        switch (fileType) {
            case XML:
                return createXMLFixture(fileName);
            case YAML:
                return createYamlFixture(fileName);
        }

        throw new FixtureException("Unsupported file type: " + fileType);
    }

    @Override
    public String getFixtureByFileName(String fileNameRelativeClassPath) {
        return getResourceFile(fileNameRelativeClassPath);
    }

    private AbstractFixture createYamlFixture(String fileName) {
        return dataSourceInfo != null ?
                new YamlFixture(dataSourceInfo, fileName, this) :
                new YamlFixture(dataSource, fileName, this);
    }

    private AbstractFixture createXMLFixture(String fileName) {
        return dataSourceInfo != null ?
                new XMLFixture(dataSourceInfo, fileName, this) :
                new XMLFixture(dataSource, fileName, this);
    }

    private String getResourceFile(String fileName) {
        InputStream resource = this.getClass().getResourceAsStream(fileName);
        if (resource == null) {
            throw new FixtureException("Resource fixture file not found: '" + fileName + "'");
        }
        try {
            return IOUtils.inputStreamToString(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FileType getFileType(String fileName) {
        String extension = fileName.contains(".") ?
                fileName.substring(fileName.lastIndexOf(".") + 1).trim().toLowerCase() :
                "";

        if (extension.equals("xml")) {
            return FileType.XML;
        } else if (extension.equals("yml")) {
            return FileType.YAML;
        } else {
            throw new IllegalArgumentException("Unsupported fixture file type: '" + fileName + "'");
        }
    }


    private enum FileType {XML, YAML}

    private DataSourceInfo dataSourceInfo;
    private DataSource dataSource; // Альтернативный источник данных.
}
