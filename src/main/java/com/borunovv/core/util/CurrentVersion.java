package com.borunovv.core.util;


/**
 * Хелпер для дебага и краткой статистики о текущей сборке проекта.
 */
public final class CurrentVersion {

    // См. pom.xml - плагин ant - он создает этот файл.
    private static final String VERSION_FILE_NAME = "version/version.txt";


    // Запретим создание экземпляра.
    private CurrentVersion() {
    }

    /**
     * Вернет уникальный идентификатор git-коммита текущей сборки.
     * Пример: 763fd078047b26f907cb1487c0620da61e931c33
     */
    public static String getGitCommitSha() {
        return getVersionFileLine(0);
    }

    private static String getVersionFileLine(int index) {
        String versionFileContent = getVersionFileContent();
        String[] lines = versionFileContent.split("\n");
        String line = index < lines.length ?
                lines[index] :
                "";
        return line.isEmpty() ?
                "[unknown]" :
                line;
    }

    private static String getVersionFileContent() {
        try {
            return ResourceManager.getTextFileContent(VERSION_FILE_NAME);
        } catch (Exception ignore) {
            return "";
        }
    }
}