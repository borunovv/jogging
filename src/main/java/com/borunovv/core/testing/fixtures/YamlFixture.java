package com.borunovv.core.testing.fixtures;

import com.borunovv.core.testing.fixtures.yaml.YamlDataSet;
import org.dbunit.dataset.IDataSet;

import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class YamlFixture extends AbstractFixture {

    public YamlFixture(DataSourceInfo dataSourceInfo,
                       String fixtureFileName,
                       IFixtureRepository fixtureRepository) {
        super(dataSourceInfo, fixtureFileName, fixtureRepository);
    }

    public YamlFixture(DataSource dataSource,
                       String fixtureFileName,
                       IFixtureRepository fixtureRepository) {
        super(dataSource, fixtureFileName, fixtureRepository);
    }

    // Шаблонный метод.
    @Override
    protected IDataSet getDataSet() {
        String fullContent = processIncludes(getFixtureFileName(), getFixtureContent());
        setFileContent(fullContent);
        return new YamlDataSet(fullContent);
    }

    /**
     * Формирует полное содержимое файла фикстуры,
     * заменяя директивы '#include(fileName)' на содержимое файлов (рекурсивно).
     */
    private String processIncludes(String fileName, String fileContent) {
        StringBuilder sb = new StringBuilder(fileContent.length());
        Matcher matcher = INCLUDE_PATTERN.matcher(fileContent);

        int start = 0;
        while (matcher.find()) {
            String includeFileName = matcher.group(1);
            String includeFileNameRelativeParent = getIncludeFileNameRelativeParent(fileName, includeFileName);

            String rawIncludeContent = getFixtureByName(includeFileNameRelativeParent);

            // Рекурсия.
            String includeContent = processIncludes(includeFileNameRelativeParent, rawIncludeContent);
            // Оставляем "---" только в корневом файле, в остальных удаляем.
            includeContent = includeContent.replace("---", "");

            String leftPart = fileContent.substring(start, matcher.start());

            sb.append(leftPart).append("\n#==BEGIN include: '").append(includeFileNameRelativeParent).append("'\n");
            sb.append(includeContent).append("\n#==END include: '").append(includeFileNameRelativeParent).append("'\n");

            start = matcher.end();
        }

        String lastPart = fileContent.substring(start);
        sb.append(lastPart);
        return sb.toString();
    }

    /**
     * Вернет имя файла фикстуры, указанной в #include(fileName.yml) относительно родительской фикстуры.
     * Example: If parent fixture: /resources/fixtures/user/users.yml
     * Then #include(aaa.yml) ->/resources/fixtures/user/aaa.yml
     *
     * @param parentFixtureFileName         путь к родительской фикстуре.
     * @param includeRelativeParentFileName имя дочерней фикстуры.
     * @return имя файла дочерне1 фикстуры относительно родительской фикстуры.
     */
    private String getIncludeFileNameRelativeParent(String parentFixtureFileName,
                                                    String includeRelativeParentFileName) {
        String result = parentFixtureFileName;

        result = fixPathDelim(result);
        result = result.contains("/") ?
                result.substring(0, result.lastIndexOf("/") + 1) :
                result;
        result = result + fixPathDelim(includeRelativeParentFileName);

        return result;
    }

    private String fixPathDelim(String path) {
        return path.replaceAll("\\\\", "/");
    }

    private static final Pattern INCLUDE_PATTERN =
            Pattern.compile("^\\s*#include\\(([a-zA-Z0-9_/\\.\\\\]+)\\)\\s*$", Pattern.MULTILINE);
}
