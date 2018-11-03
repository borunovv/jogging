package com.borunovv.core.testing.fixtures;

import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;

import javax.sql.DataSource;
import java.io.StringReader;

public class XMLFixture extends AbstractFixture {

    public XMLFixture(DataSourceInfo dataSourceInfo,
                      String fixtureFileName,
                      IFixtureRepository fixtureRepository) {
        super(dataSourceInfo, fixtureFileName, fixtureRepository);
    }

    public XMLFixture(DataSource dataSource,
                      String fixtureFileName,
                      IFixtureRepository fixtureRepository) {
        super(dataSource, fixtureFileName, fixtureRepository);
    }

    // Шаблонный метод.
    @Override
    protected IDataSet getDataSet() {
        try {
            String xmlFileContent = getFixtureContent();
            return new FlatXmlDataSetBuilder().build(new StringReader(xmlFileContent));
        } catch (DataSetException e) {
            throw makeException("Failed to create DataSet for XML file.", e);
        }
    }
}
