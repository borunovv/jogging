package com.borunovv.core.testing;

import com.borunovv.core.testing.fixtures.AbstractFixture;
import com.borunovv.core.testing.fixtures.DataSourceInfo;
import com.borunovv.core.testing.fixtures.FixtureFactory;
import org.junit.After;
import org.junit.Before;

import javax.sql.DataSource;
import java.util.Stack;

public abstract class AbstractTestWithFixture {

    @Before
    public void setUp() throws Exception {
        if (initFixtureFactory()) {
            loadFixtures();
        }
        onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        onTearDown();
        tearDownFixtures();
    }

    protected DataSourceInfo getDataSourceInfo() {
        return null;
    }

    protected DataSource getDataSource() {
        return null;
    }

    protected FixtureInfo[] getFixtures() {
        return null;
    }

    protected String getFixture() {
        return null;
    }

    protected void onSetUp() {
    }

    protected void onTearDown() {
    }

    private boolean initFixtureFactory() {
        if (fixtureFactory == null) {
            DataSourceInfo dataSourceInfo = getDataSourceInfo();
            DataSource dataSource = getDataSource();

            if (dataSource != null) {
                fixtureFactory = new FixtureFactory(dataSource);
            } else if (dataSourceInfo != null) {
                fixtureFactory = new FixtureFactory(dataSourceInfo);
            } else {
                log("INFO: Fixtures are NOT available for this test."
                        + " Reason: data source wasn't set"
                        + " (did you forget to overload getDataSourceInfo() method ?).");
            }
        }
        return fixtureFactory != null;
    }

    private void loadFixtures() throws Exception {
        FixtureInfo[] fixtureInfos = getFixtures();
        if (fixtureInfos == null) {
            String singleFixturFileName = getFixture();
            if (singleFixturFileName != null) {
                fixtureInfos = new FixtureInfo[]{new FixtureInfo(singleFixturFileName)};
            }
        }

        if (fixtureInfos != null) {
            log("=== Loading fixtures.");
            for (FixtureInfo info : fixtureInfos) {
                setUpFixture(info);
                log("    fixture: " + info + " - OK.");
            }
        }
    }

    protected void setUpFixture(FixtureInfo fixtureInfo) throws Exception {
        String relativeFileName = getRelativeFixturesDirFileName(fixtureInfo.getFileName());
        AbstractFixture fixture = fixtureFactory.create(relativeFileName);
        fixture.setUp(fixtureInfo.getSetupType());
        fixtures.push(fixture);
    }

    protected void tearDownFixtures() throws Exception {
        if (fixtures.isEmpty()) return;
        log("=== TearDown fixtures.");
        while (!fixtures.isEmpty()) {
            AbstractFixture fixture = fixtures.pop();
            fixture.tearDown();
        }
    }

    private String getRelativeFixturesDirFileName(String fileName) {
        return "/fixtures/" + fileName;
    }

    protected class FixtureInfo {
        private String fileName;
        private AbstractFixture.SetupType setupType;

        public FixtureInfo(String fileName, AbstractFixture.SetupType setupType) {
            this.fileName = fileName;
            this.setupType = setupType;
        }

        public FixtureInfo(String fileName) {
            this(fileName, AbstractFixture.SetupType.CLEAN_INSERT);
        }

        public String getFileName() {
            return fileName;
        }

        public AbstractFixture.SetupType getSetupType() {
            return setupType;
        }

        @Override
        public String toString() {
            return "'" + fileName + "' (" + setupType.toString() + ")";
        }
    }

    /**
     * Enable fixture init/rollback logging messages in system out (disabled by default).
     * Note: Use in setUp() method in derived class (don't forget to call super.setUp() after).
     */
    protected void enableLogging() {
        loggingEnabled = true;
    }

    private void log(String msg) {
        if (loggingEnabled) {
            System.out.println(msg);
        }
    }

    private Stack<AbstractFixture> fixtures = new Stack<AbstractFixture>();
    private static FixtureFactory fixtureFactory;
    private boolean loggingEnabled = false;
}