package org.nors.dev.codes.lpu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.gate-attendance")
public class GateAttendanceProperties {

    /** IP or hostname of the gate attendance Postgres server. */
    private String dbHost = "127.0.0.1";
    private int dbPort = 5432;
    private String dbName = "postgres";
    private String dbUsername = "postgres";
    private String dbPassword = "";
    /** Optional gate web base URL for photos. Empty → http://{dbHost} */
    private String url = "";

    public String jdbcUrl() {
        return "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
    }

    public String resolvedWebUrl() {
        if (url != null && !url.isBlank()) {
            return url.replaceAll("/+$", "");
        }
        return "http://" + dbHost;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
