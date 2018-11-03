package com.borunovv.core.testing.fixtures;

public final class DataSourceInfo {

    private String driverClass;
    private String url;
    private String userName;
    private String password;

    public DataSourceInfo(String driverClass, String url, String userName, String password) {
        this.driverClass = driverClass;
        this.url = url;
        this.userName = userName;
        this.password = password;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getUrl() {
        return url;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "DataSourceInfo{" +
                "driverClass='" + driverClass + '\'' +
                ", url='" + url + '\'' +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
