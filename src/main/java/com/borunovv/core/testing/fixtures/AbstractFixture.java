package com.borunovv.core.testing.fixtures;

import com.borunovv.core.util.StringUtils;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.springframework.util.Assert;

import javax.sql.DataSource;

public abstract class AbstractFixture {

    public AbstractFixture(DataSourceInfo dataSourceInfo,
                           String fixtureFileName,
                           IFixtureRepository fixtureRepository) {

        Assert.notNull(dataSourceInfo);
        Assert.notNull(fixtureFileName);
        Assert.notNull(fixtureRepository);
        this.fixtureRepository = fixtureRepository;
        this.fixtureFileName = fixtureFileName;
        this.fixtureFileContent = getFixtureByName(fixtureFileName);

        try {
            databaseTester = new JdbcDatabaseTester(
                    dataSourceInfo.getDriverClass(),
                    dataSourceInfo.getUrl(),
                    dataSourceInfo.getUserName(),
                    dataSourceInfo.getPassword());
        } catch (Exception e) {
            throw makeException("Failed to initialize fixture.\nParams:"
                    + "\n\ndataSourceInfo=" + dataSourceInfo, e);
        }
    }

    public AbstractFixture(DataSource dataSource,
                           String fixtureFileName,
                           IFixtureRepository fixtureRepository) {

        Assert.notNull(dataSource);
        Assert.notNull(fixtureFileName);
        Assert.notNull(fixtureRepository);
        this.fixtureRepository = fixtureRepository;
        this.fixtureFileName = fixtureFileName;
        this.fixtureFileContent = getFixtureByName(fixtureFileName);

        try {
            databaseTester = new DataSourceDatabaseTester(dataSource);
        } catch (Exception e) {
            throw makeException("Failed to initialize fixture.\nParams:"
                    + "\n\ndataSource=" + dataSource, e);
        }
    }

    // Шаблонный метод.
    protected abstract IDataSet getDataSet();

    public void setUp() {
        setUp(SetupType.CLEAN_INSERT);
    }

    public void setUp(SetupType type) {
        try {
            if (dataSet == null) {
                dataSet = getDataSet();
                databaseTester.setDataSet(dataSet);
            }

            databaseTester.setSetUpOperation(type.getDatabaseOperation());
            databaseTester.onSetup();
        } catch (Exception e) {
            throw makeException("Fixture setUp() failed.", e);
        }
    }

    public void tearDown() {
        try {
            databaseTester.setTearDownOperation(DatabaseOperation.DELETE_ALL);// Чистит за собой таблицы.
            databaseTester.onTearDown();
        } catch (Exception e) {
            throw makeException("Fixture tearDown() failed.", e);
        }
    }

    // Хелпер для создания информативного исключения.
    protected FixtureException makeException(String msg, Throwable cause) {
        String fileNameAndContent = "Fixture file name: '" + fixtureFileName + "'\n"
                + "Content:\n" + getFileContentWithLineIndexes();

        String fullMsg = StringUtils.isNullOrEmpty(msg) ?
                fileNameAndContent :
                msg + "\n" + fileNameAndContent;

        return cause == null ?
                new FixtureException(fullMsg) :
                new FixtureException(fullMsg, cause);
    }

    protected FixtureException makeException(String msg) {
        return makeException(msg, null);
    }

    protected String getFixtureFileName() {
        return fixtureFileName;
    }

    protected String getFixtureContent() {
        return getFixtureByName(fixtureFileName);
    }

    protected void setFileContent(String content) {
        fixtureFileContent = content;
    }

    protected String getFixtureByName(String relativeClassPathFileName) {
        return fixtureRepository.getFixtureByFileName(relativeClassPathFileName);
    }

    private String getFileContentWithLineIndexes() {
        String[] lines = fixtureFileContent.split("\\n");
        StringBuilder sb = new StringBuilder(fixtureFileContent.length() * 2);
        int lineIndex = 1;
        final String spaces = "        ";
        for (String line : lines) {
            String lineIndexStr = "" + lineIndex + ":";
            lineIndexStr = lineIndexStr + spaces.substring(0, spaces.length() - lineIndexStr.length());
            sb.append(lineIndexStr).append(line).append("\n");
            lineIndex++;
        }
        return sb.toString();
    }

    public enum SetupType {
        CLEAN_INSERT(DatabaseOperation.CLEAN_INSERT), // Сначала очистка таблицы, затем вставка данных.
        INSERT(DatabaseOperation.INSERT);             // Просто вставка данных.

        SetupType(DatabaseOperation op) {
            operation = op;
        }

        public DatabaseOperation getDatabaseOperation() {
            return operation;
        }

        private DatabaseOperation operation;
    }

    private IDatabaseTester databaseTester;
    private IFixtureRepository fixtureRepository;
    private IDataSet dataSet;
    private String fixtureFileName;    // Для информативности исключений.
    private String fixtureFileContent; // Тоже для информативности и поиска ошибок (особенно после препроцессинга '#include').
}
