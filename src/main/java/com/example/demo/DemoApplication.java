package com.example.demo;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DemoApplication {

    private static final Logger log;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%4$-7s] %5$s %n");
        log =Logger.getLogger(DemoApplication.class.getName());
    }

    public static void main(String[] args) throws Exception {
        String dispurl;
        log.info("Loading application properties");
        EnvProperties properties = new EnvProperties();
        properties.load(DemoApplication.class.getClassLoader().getResourceAsStream("application.properties"));
        dispurl = properties.getProperty("url");
        System.out.println("This is url in application.properties: " + dispurl);
        

        log.info("Connecting to the database");
        Connection connection = DriverManager.getConnection(properties.getProperty("url"), properties);
        log.info("Database connection test: " + connection.getCatalog());

        log.info("Create database schema");
        Scanner scanner = new Scanner(DemoApplication.class.getClassLoader().getResourceAsStream("schema.sql"));
        Statement statement = connection.createStatement();
        while (scanner.hasNextLine()) {
            statement.execute(scanner.nextLine());
        }

		/*
        */
        
		Todo todo = new Todo(1L, "configuration", "congratulations, you have set up JDBC correctly!", true);
        insertData(todo, connection);
        todo = readData(connection);
        todo.setDetails("congratulations, you have updated data!");
        updateData(todo, connection);
        deleteData(todo, connection);
		

        log.info("Closing database connection");
        connection.close();
    }

    private static void insertData(Todo todo, Connection connection) throws SQLException {
        log.info("Insert data");
        PreparedStatement insertStatement = connection
                .prepareStatement("INSERT INTO todo (id, description, details, done) VALUES (?, ?, ?, ?);");
    
        insertStatement.setLong(1, todo.getId());
        insertStatement.setString(2, todo.getDescription());
        insertStatement.setString(3, todo.getDetails());
        insertStatement.setBoolean(4, todo.isDone());
        insertStatement.executeUpdate();
    }

    private static Todo readData(Connection connection) throws SQLException {
        log.info("Read data");
        PreparedStatement readStatement = connection.prepareStatement("SELECT * FROM todo;");
        ResultSet resultSet = readStatement.executeQuery();
        if (!resultSet.next()) {
            log.info("There is no data in the database!");
            return null;
        }
        Todo todo = new Todo();
        todo.setId(resultSet.getLong("id"));
        todo.setDescription(resultSet.getString("description"));
        todo.setDetails(resultSet.getString("details"));
        todo.setDone(resultSet.getBoolean("done"));
        log.info("Data read from the database: " + todo.toString());
        return todo;
    }

    private static void updateData(Todo todo, Connection connection) throws SQLException {
        log.info("Update data");
        PreparedStatement updateStatement = connection
                .prepareStatement("UPDATE todo SET description = ?, details = ?, done = ? WHERE id = ?;");
    
        updateStatement.setString(1, todo.getDescription());
        updateStatement.setString(2, todo.getDetails());
        updateStatement.setBoolean(3, todo.isDone());
        updateStatement.setLong(4, todo.getId());
        updateStatement.executeUpdate();
        readData(connection);
    }

    private static void deleteData(Todo todo, Connection connection) throws SQLException {
        log.info("Delete data");
        PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM todo WHERE id = ?;");
        deleteStatement.setLong(1, todo.getId());
        deleteStatement.executeUpdate();
        readData(connection);
    }

}

class EnvProperties extends Properties
{
    private static final long serialVersionUID = 1L;
    private Pattern envVarPattern = Pattern.compile("\\$\\{([^}]+)\\}");
    
    @Override
    public String getProperty(String key)
    {
    	String value = super.getProperty(key);
        if (null == value || value.isBlank())
            return null;
        StringBuffer result = new StringBuffer(value.length());
        Matcher m = envVarPattern.matcher(value);
        while (m.find())
        {
            String envValue = System.getenv(m.group(1));
            m.appendReplacement(result, null == envValue ? "" : envValue);
        }
        m.appendTail(result);
        return result.toString();
    }
}
